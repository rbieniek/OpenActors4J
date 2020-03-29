package io.openactors4j.core.impl.common;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;


import io.openactors4j.core.common.Actor;
import io.openactors4j.core.common.DeathNote;
import io.openactors4j.core.common.Signal;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.impl.messaging.ExtendedMessage;
import io.openactors4j.core.impl.messaging.Message;
import io.openactors4j.core.impl.messaging.RoutingSlip;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class models the actual actor instance held in the tree of actors inside the
 * actor system.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Getter(AccessLevel.PROTECTED)
@Slf4j
@SuppressWarnings( {"PMD.TooManyMethods", "PMD.UnusedFormalParameter", "TooManyStaticImports"})
public abstract class ActorInstance<V extends Actor, T> implements ActorInstanceStateTransition {

  private final ActorInstanceContext context;
  private final Callable<V> instanceSupplier;
  @Getter(AccessLevel.PUBLIC)
  private final String name;
  private final SupervisionStrategyInternal supervisionStrategy;
  private final StartupMode startupMode;

  private final Map<String, ActorInstance> childActors = new ConcurrentHashMap<>();

  @Getter(AccessLevel.PROTECTED)
  private V instance;

  @Getter(AccessLevel.PROTECTED)
  private ActorContextImpl actorContext;

  private Consumer<Message<T>> deathNoteHandler = this::standardDeathNoteHandler;

  private ActorInstanceStateMachine stateMachine = ActorInstanceStateMachine.newInstance()
      .presetStateMatrix(this::noShift)
      .addState(InstanceState.NEW, InstanceState.CREATING, this::createNewInstance)
      .addState(InstanceState.NEW, InstanceState.CREATE_DELAYED, this::shift)
      .addState(InstanceState.CREATING, InstanceState.STARTING, this::startNewInstance)
      .addState(InstanceState.CREATING, InstanceState.CREATE_FAILED, this::executeSupervisionStrategy)
      .addState(InstanceState.CREATE_FAILED, InstanceState.STOPPED, this::terminateInstance)
      .addState(InstanceState.STARTING, InstanceState.RUNNING, this::shift)
      .addState(InstanceState.STARTING, InstanceState.START_FAILED, this::executeSupervisionStrategy)
      .addState(InstanceState.START_FAILED, InstanceState.STOPPING, this::stopInstance)
      .addState(InstanceState.RUNNING, InstanceState.PROCESSING_FAILED, this::executeSupervisionStrategy)
      //      .addState(InstanceState.NEW, InstanceState.RUNNING, this::startNewInstance)
//      .addState(InstanceState.DELAYED, InstanceState.RUNNING, this::startDelayedInstance)
//      .addState(InstanceState.DELAYED, InstanceState.STOPPED, this::stopInstance)
//      .addState(InstanceState.RESTARTING, InstanceState.STOPPED, this::stopInstance)
//      .addState(InstanceState.RESTARTING_DELAYED, InstanceState.STOPPED, this::stopInstance)
//      .addState(InstanceState.RUNNING, InstanceState.STOPPED, this::stopInstance)
//      .addState(InstanceState.STARTING, InstanceState.STOPPED, this::stopInstance)
//      .addState(InstanceState.RUNNING, InstanceState.SUSPENDED, this::suspendInstance)
//      .addState(InstanceState.STARTING, InstanceState.RUNNING, this::startComplete)
//      .addState(InstanceState.STARTING, InstanceState.RESTARTING_DELAYED, this::startFailed)
//      .addState(InstanceState.RUNNING, InstanceState.RESTARTING, this::restartInstance)
//      .addState(InstanceState.RESTARTING, InstanceState.RUNNING, this::startComplete)
//      .addState(InstanceState.STARTING, InstanceState.RESTARTING,this::restartInstance)
      .addState(InstanceState.STOPPING, InstanceState.STOPPED, this::terminateInstance);

  /**
   * Handle a message which is destined for this actor:
   *
   * @param message the message to process;
   */
  @SuppressWarnings("PMD.SignatureDeclareThrowsException")
  protected abstract void handleMessage(final Message<T> message) throws Exception;

  /**
   * Handle signal reception
   */
  @SuppressWarnings( {"PMD.SignatureDeclareThrowsException", "PMD.AvoidUncheckedExceptionsInSignatures"})
  protected abstract void sendSignal(Signal signal) throws ActorInstanceSignalingFailedException;

  /**
   * Trigger the creation of the enclosed actor.
   * <p>
   * Depending on startup mode, the creation will start immediately or will be delayed until the
   * reception of the first message
   */
  public void triggerActorCreation() {
    stateMachine.start();

    if (startupMode == StartupMode.IMMEDIATE) {
      transitionState(InstanceState.CREATING);
    } else {
      transitionState(InstanceState.CREATE_DELAYED);
    }
  }

  /**
   * Move the actor instance to a new state.
   */
  public void transitionState(final InstanceState desiredState) {
    stateMachine.transitionState(desiredState);
  }

  public void parentalLifecycleEvent(final ParentLifecycleEvent lifecycleEvent) {
    childActors.values()
        .forEach(actorInstance -> actorInstance.parentalLifecycleEvent(lifecycleEvent));

    stateMachine.parentalLifecycleEvent(lifecycleEvent);
  }

  public InstanceState getInstanceState() {
    return stateMachine.getInstanceState();
  }

  /**
   * Lookup an actor instance from the path component of a given {@link RoutingSlip}.
   *
   * @param routingSlip message routing slip
   * @return an {@link Optional} to an {@link ActorInstance} i
   */
  public Optional<ActorInstance> lookupActorInstance(final RoutingSlip routingSlip) {
    return empty();
  }

  /**
   * Route the incoming message according to its path:
   *
   * <ul>
   *   <li>If the actor state machine permits permits message reception,
   *   the message is processed by the subsequent steps. Otherwise, it is passed in to the
   *   {@link ActorInstanceContext#undeliverableMessage(Message)} method</li>
   * <li>if the target routing slip has q child path, route the message to the child actor with the
   * name denoted by next path part. If no child actor with a matching name can be found, send the
   * message to the systems unreachable handler</li>
   * <li>if the message if targeted to this actor, enqueue the message into the mailbox</li>
   * </ul>
   *
   * <p>If the message is destined for this instance, the message is enqueued.
   * <b>Please note:</b> If the actor is created with delayed startup, the actor is
   * scheduled for starting
   *
   * @param message the message to be routed
   */
  public void routeMessage(final Message<T> message) {
    final Optional<String> currentPart = message.getTarget().nextPathPart();

    if (stateMachine.messageReceptionEnabled()) {
      currentPart.ifPresentOrElse(pathPath -> {
        ofNullable(childActors.get(pathPath))
            .ifPresentOrElse(child -> child.routeMessage(message),
                () -> {
                  log.info("Actor {} undeliverable message to {}", name, pathPath);

                  context.undeliverableMessage(message);
                });
      }, () -> {
        if (message instanceof ExtendedMessage) {
          final ExtendedMessage<T, ?> extendedMessage = (ExtendedMessage<T, ?>) message;

          if (extendedMessage.getExtensionData() instanceof DeathNote) {
            log.info("Actor {} received death note", name);

            deathNoteHandler.accept(message);
          } else {
            log.info("Actor {} received unhandleable extension data {}", name,
                extendedMessage.getExtensionData());

            context.undeliverableMessage(message);
          }
        } else {
          context.enqueueMessage(message);

          switch (stateMachine.getInstanceState()) {
            case RUNNING:
              context.scheduleMessageProcessing();
              break;
            case CREATE_DELAYED:
              transitionState(InstanceState.CREATING);
              break;
            default:
          }
        }
      });
    } else {
      context.undeliverableMessage(message);
    }
  }

  private void standardDeathNoteHandler(final Message<T> message) {
    log.info("Actor {} received death note", name);

    childActors.values().forEach(child -> child.routeMessage(message));

    transitionState(InstanceState.STOPPED);
  }

  private void systemActorDeathNoteHandler(final Message<T> message) {
    log.info("Actor {} ignores received death note", name);

    context.undeliverableMessage(message);
  }

  /**
   * handle the next message in the mailbox:
   */
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public void handleNextMessage(final Message<T> message) {
    try {
      actorContext.setCurrentSender(context.actorRefForAddress(message.getSender()));

      handleMessage(message);
    } catch (Exception e) {
      log.info("Actor {} caught exception in message processing", name, e);

      supervisionStrategy.handleMessageProcessingException(e,
          new WeakActorInstanceStateTransition<>(this),
          context);
    } finally {
      actorContext.setCurrentSender(null);
    }

  }

  /**
   * Create the actor implementation instance:
   */
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private void createEnclosedActor() {
    try {
      this.instance = instanceSupplier.call();
      this.instance.setupContext(this.actorContext);

      if (this.instance instanceof SystemActor) {
        log.info("Enabling system actor behavior for actor {}", name);

        deathNoteHandler = this::systemActorDeathNoteHandler;
      }
    } catch (Exception e) {
      log.info("Caught exception on actor {} instance creation", name, e);

      throw new ActorInstanceCreationFailedException(e);
    }
  }

  private WeakActorInstanceStateTransition<V, T> weakReference() {
    return new WeakActorInstanceStateTransition<>(this);
  }

  /**
   * Attempt to create the enclosed actor actor.
   *
   * @param sourceState the state before applying the transition function
   * @param targetState the desired state, ignored in this case
   * @return the target state to move the state machine to
   */
  @SuppressWarnings( {"PMD.AvoidFinalLocalVariable"})
  private void createNewInstance(final InstanceState sourceState,
                                 final InstanceState targetState,
                                 final Optional<ActorInstanceStateMachine.TransitionContext> transitionContext) {
    this.actorContext = new ActorContextImpl(context);

    context.runAsync(() -> createEnclosedActor())
        .whenComplete((value, throwable) -> transitionConditionallyOnException((Throwable) throwable,
            InstanceState.STARTING, InstanceState.CREATE_FAILED));
  }

  private void startNewInstance(final InstanceState sourceState,
                                final InstanceState targetState,
                                final Optional<ActorInstanceStateMachine.TransitionContext> transitionContext) {
    context.runAsync(() -> sendSignal(Signal.PRE_START))
        .whenComplete((value, throwable) -> transitionConditionallyOnException((Throwable) throwable,
            InstanceState.RUNNING, InstanceState.START_FAILED));
  }

  private void executeSupervisionStrategy(final InstanceState sourceState,
                                          final InstanceState targetState,
                                          final Optional<ActorInstanceStateMachine.TransitionContext> transitionContext) {
    final ActorInstanceStateMachine.TransitionContext ctx = transitionContext
        .orElseThrow(() -> new IllegalArgumentException("Expected transaction context on executing supervision strategy for actor {}" + name));

    switch (sourceState) {
      case CREATING:
        supervisionStrategy.handleActorCreationException(ctx.getThrowable(),
            weakReference(),
            getContext());
        break;
      case STARTING:
        supervisionStrategy.handleSignalProcessingException(ctx.getThrowable(),
            ctx.getSignal(),
            weakReference(),
            getContext());
        break;
      case RUNNING:
        supervisionStrategy.handleMessageProcessingException(transitionContext.get().getThrowable(),
            weakReference(),
            getContext());
        break;
      default:
        log.info("cannot execute supervision strategy on transition from source state {} to target state {}", sourceState, targetState);
        throw new IllegalStateException("No supervision strategy for transition from source state"
            + sourceState + " to target state " + targetState);
    }
  }

  private void transitionConditionallyOnException(final Throwable throwable,
                                                  final InstanceState onSuccessState,
                                                  final InstanceState onExceptionState) {
    Optional.ofNullable(throwable).ifPresentOrElse(toThrow -> {
      log.info("Actor name {} caught exception while transition from state {}",
          name, stateMachine.getInstanceState(), throwable);

      stateMachine.transitionState(onExceptionState, toThrow);
    }, () -> stateMachine.transitionState(onExceptionState));
  }

  private void terminateInstance(final InstanceState sourceState, final InstanceState targetState,
                                 final Optional<ActorInstanceStateMachine.TransitionContext> transitionContext) {
    stateMachine.stop();
  }

  private void stopInstance(final InstanceState sourceState, final InstanceState targetState,
                                 final Optional<ActorInstanceStateMachine.TransitionContext> transitionContext) {

    context.runAsync(() -> sendSignal(Signal.POST_STOP))
        .thenRun(() -> stateMachine.transitionState(InstanceState.STOPPED));
  }


  private void noShift(final InstanceState fromState, final InstanceState toState,
                       final Optional<ActorInstanceStateMachine.TransitionContext> transitionContext) {
  }

  private void shift(final InstanceState fromState, final InstanceState toState,
                     final Optional<ActorInstanceStateMachine.TransitionContext> transitionContext) {
    stateMachine.transitionState(toState, transitionContext);
  }
}
