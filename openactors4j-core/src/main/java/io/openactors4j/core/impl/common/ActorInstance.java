package io.openactors4j.core.impl.common;

import static java.util.Optional.empty;
import static java.util.Optional.of;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * This class models the actual actor instance held in the tree of actors inside the
 * actor system.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Getter(AccessLevel.PROTECTED)
@Slf4j
@SuppressWarnings( {"PMD.TooManyMethods", "PMD.UnusedFormalParameter", "TooManyStaticImports"})
public abstract class ActorInstance<V extends Actor, T> {

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
      .presetStateMatrix(ActorInstanceStateMachine::noShift)
      .addState(InstanceState.NEW, InstanceState.CREATING, this::createNewInstance)
      .addState(InstanceState.NEW, InstanceState.CREATE_DELAYED, ActorInstanceStateMachine::shift)
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
      .addState(InstanceState.STOPPING, InstanceState.STOPPED, ActorInstanceStateMachine::shift);

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
  protected abstract void sendSignal(Signal signal) throws RuntimeException;

  /**
   * Trigger the creation of the enclosed actor.
   *
   * Depending on startup mode, the creation will start immediately or will be delayed until the
   * reception of the first message
   */
  public void triggerActorCreation() {
    if(startupMode == StartupMode.IMMEDIATE) {
      transitionState(InstanceState.CREATING);
    } else {
      transitionState(InstanceState.CREATE_DELAYED);
    }
  }

  /**
   * Move the actor instance to a new state.
   */
  public void transitionState(final InstanceState desiredState) {
    stateMachine.transitionState(desiredState, name);
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

      transitionState(supervisionStrategy.handleMessageProcessingException(e, this, context));
    } finally {
      actorContext.setCurrentSender(null);
    }

  }

  /**
   * Create the actor implementation instance:
   */
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  @SneakyThrows
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

      throw e;
    }
  }

  /**
   * Attempt to create the enclosed actor actor.
   *
   * @param desiredState the desired state, ignored in this case
   * @return the target state to move the state machine to
   */
  @SuppressWarnings( {"PMD.AvoidFinalLocalVariable"})
  private Optional<InstanceState> createNewInstance(final InstanceState desiredState) {
    final InstanceState resultState;

    this.actorContext = new ActorContextImpl(context);

    context.runAsync(this::createEnclosedActor).whenComplete((result, throwable) -> {
      if(throwable != null) {
        log.info("Failed to create the embedded actor for actor instance {}", name, throwable);
        transitionState(InstanceState.CREATE_FAILED);
      } else {
        transitionState(InstanceState.STARTING);
      }
    });

    return empty();
  }

  private Optional<InstanceState> startDelayedInstance(final InstanceState desiredState) {
    instanceState = InstanceState.STARTING;

    context.runAsync(this::createEnclosedActor)
        .handle((s, t) -> sendPreStartSignalAfterCreation(s, (Throwable) t))
        .handle((s, t) -> decideStateAfterInstanceStart(Signal.PRE_START, (Throwable) t))
        .whenComplete((state, throwable) -> transitionState((InstanceState) state));

    return empty();
  }


  @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
  private InstanceState decideStateAfterInstanceStart(final Signal signal, final Throwable throwable) {
    InstanceState result = InstanceState.RUNNING;

    if (throwable != null) {
      result = supervisionStrategy.handleSignalProcessingException(throwable, signal, this, context);
    }

    return result;
  }

  @SneakyThrows
  private Object sendPreStartSignalAfterCreation(final Object object, final Throwable throwable) {

    if (throwable == null) {
      sendSignal(Signal.PRE_START);
    } else {
      throw throwable;
    }

    return object;
  }

  private Optional<InstanceState> stopInstance(final InstanceState desiredState) {
    context.runAsync(() -> sendSignal(Signal.POST_STOP))
        .whenComplete((state, throwable) -> {
          if (throwable != null) {
            log.info("Actor failed to process stop signal",
                throwable);
          }
        });

    context.actorInstanceStopped();

    return of(InstanceState.STOPPED);
  }

  private Optional<InstanceState> suspendInstance(final InstanceState desiredState) {

    return of(InstanceState.STOPPED);
  }

  private Optional<InstanceState> startComplete(final InstanceState desiredState) {
    context.scheduleMessageProcessing();

    return of(desiredState);
  }


  private Optional<InstanceState> startFailed(final InstanceState desiredState) {

    return of(desiredState);
  }

  private Optional<InstanceState> restartInstance(final InstanceState desiredState) {
    context.runAsync(() -> sendSignal(Signal.PRE_RESTART))
        .handle((s, t) -> decideStateAfterInstanceStart(Signal.PRE_START, (Throwable) t))
        .whenComplete((state, throwable) -> transitionState((InstanceState) state));
    return empty();
  }

}
