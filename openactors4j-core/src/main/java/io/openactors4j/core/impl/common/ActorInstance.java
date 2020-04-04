package io.openactors4j.core.impl.common;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;


import io.openactors4j.core.common.Actor;
import io.openactors4j.core.common.DeathNote;
import io.openactors4j.core.common.Signal;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.impl.common.ActorInstanceStateMachine.ActorInstanceTransitionContext;
import io.openactors4j.core.impl.common.ReactiveStateMachine.ReactiveStateUpdater;
import io.openactors4j.core.impl.messaging.ExtendedMessage;
import io.openactors4j.core.impl.messaging.Message;
import io.openactors4j.core.impl.messaging.RoutingSlip;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import io.openactors4j.core.monitoring.ActorActionEvent;
import io.openactors4j.core.monitoring.ActorActionEventSubscriber;
import io.openactors4j.core.monitoring.ActorActionEventType;
import io.openactors4j.core.monitoring.ActorOutcomeType;
import io.openactors4j.core.monitoring.ActorSignalEvent;
import io.openactors4j.core.monitoring.ActorSignalEventSubscriber;
import io.openactors4j.core.monitoring.ActorSignalType;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
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
public abstract class ActorInstance<V extends Actor, T> {

  private final Callable<V> instanceSupplier;
  @Getter(AccessLevel.PUBLIC)
  private final String name;
  private final SupervisionStrategyInternal supervisionStrategy;
  private final StartupMode startupMode;

  private Map<String, ActorInstance> childActors = new ConcurrentHashMap<>();

  private ActorInstanceContext context;

  @Getter(AccessLevel.PROTECTED)
  private V instance;

  @Getter(AccessLevel.PROTECTED)
  private ActorContextImpl actorContext;

  private Consumer<Message<T>> deathNoteHandler = this::standardDeathNoteHandler;

  private ActorInstanceStateMachine stateMachine;

  private SubmissionPublisher<ActorActionEvent> monitoringActionPublisher;
  private SubmissionPublisher<ActorSignalEvent> monitoringSignalPublisher;

  public void initializeAndStart(final ActorInstanceContext context) {
    this.context = context;

    this.stateMachine = context.provideStateMachine(name);

    this.stateMachine
        .addState(InstanceState.NEW, InstanceState.CREATING, this::createNewInstance)
        .addState(InstanceState.NEW, InstanceState.CREATE_DELAYED, this::noAction)
        .addState(InstanceState.CREATING, InstanceState.STARTING, this::startNewInstance)
        .addState(InstanceState.CREATING, InstanceState.CREATE_FAILED, this::executeSupervisionStrategy)
        .addState(InstanceState.CREATE_FAILED, InstanceState.STOPPED, this::terminateInstance)
        .addState(InstanceState.CREATE_FAILED, InstanceState.CREATING, this::createNewInstance)
        .addState(InstanceState.CREATE_DELAYED, InstanceState.CREATING, this::createNewInstance)
        .addState(InstanceState.STARTING, InstanceState.RUNNING, this::enableMessageDelivery)
        .addState(InstanceState.STARTING, InstanceState.START_FAILED, this::executeSupervisionStrategy)
        .addState(InstanceState.START_FAILED, InstanceState.STOPPING, this::stopInstance)
        .addState(InstanceState.START_FAILED, InstanceState.STARTING, this::startNewInstance)
        .addState(InstanceState.RUNNING, InstanceState.PROCESSING_FAILED, this::executeSupervisionStrategy)
        .addState(InstanceState.RUNNING, InstanceState.STOPPING, this::stopInstance)
        .addState(InstanceState.PROCESSING_FAILED, InstanceState.RESTARTING, this::restartInstance)
        .addState(InstanceState.RESTARTING, InstanceState.RESTART_FAILED, this::executeSupervisionStrategy)
        .addState(InstanceState.RESTART_FAILED, InstanceState.STOPPING, this::stopInstance)
        .addState(InstanceState.RESTART_FAILED, InstanceState.RESTARTING, this::restartInstance)
        .addState(InstanceState.RESTARTING, InstanceState.RUNNING, this::enableMessageDelivery)
        .addState(InstanceState.STOPPING, InstanceState.STOPPED, this::terminateInstance)
        .setDefaultAction(this::noAction)
        .start(InstanceState.NEW);

    monitoringActionPublisher = new SubmissionPublisher<>(context.provideMonitoringExecutor(),
        Flow.defaultBufferSize());
    monitoringSignalPublisher = new SubmissionPublisher<>(context.provideMonitoringExecutor(),
        Flow.defaultBufferSize());

    context.assignAndCreate(this);
    context.disableMessageDelivery();
  }

  /**
   * Handle a message which is destined for this actor:
   *
   * @param message the message to process;
   */
  @SuppressWarnings("PMD.SignatureDeclareThrowsException")
  protected abstract void sendMessageToActor(final Message<T> message) throws Exception;

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
    if (startupMode == StartupMode.IMMEDIATE) {
      stateMachine.postStateTransition(InstanceState.CREATING);
    } else {
      stateMachine.postStateTransition(InstanceState.CREATE_DELAYED);
    }
  }

  public void parentalLifecycleEvent(final ParentLifecycleEvent lifecycleEvent) {
    childActors.values()
        .forEach(actorInstance -> actorInstance.parentalLifecycleEvent(lifecycleEvent));

    stateMachine.parentalLifecycleEvent(lifecycleEvent);
  }

  public InstanceState getInstanceState() {
    return stateMachine.getCurrentState();
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
          if (stateMachine.getCurrentState() == InstanceState.CREATE_DELAYED) {
            stateMachine.postStateTransition(InstanceState.CREATING);
          }
        }
      });
    } else {
      context.undeliverableMessage(message);
    }
  }

  public void registerActionEventSubscriber(final ActorActionEventSubscriber subscriber) {
    this.monitoringActionPublisher.subscribe(subscriber);
  }

  public void registerSignalEventSubscriber(final ActorSignalEventSubscriber subscriber) {
    this.monitoringSignalPublisher.subscribe(subscriber);
  }

  private void standardDeathNoteHandler(final Message<T> message) {
    log.info("Actor {} received death note", name);

    childActors.values().forEach(child -> child.routeMessage(message));

    stateMachine.postStateTransition(InstanceState.STOPPING);
  }

  private void systemActorDeathNoteHandler(final Message<T> message) {
    log.info("Actor {} ignores received death note", name);

    context.undeliverableMessage(message);
  }

  /**
   * handle the next message in the mailbox:
   */
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public void processMessage(final Message<T> message) {
    final ZonedDateTime now = ZonedDateTime.now();

    try {
      actorContext.setCurrentSender(context.actorRefForAddress(message.getSender()));

      sendMessageToActor(message);

      publishActionEvent(ActorActionEventType.MESSAGE_DELIVERY, now, ActorOutcomeType.SUCCESS);
    } catch (Exception e) {
      log.info("Actor {} caught exception in message processing", name, e);

      supervisionStrategy.handleMessageProcessingException(e,
          weakReference(),
          context);
      publishActionEvent(ActorActionEventType.MESSAGE_DELIVERY, now, ActorOutcomeType.FAILURE);
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

  private WeakActorInstanceStateTransition weakReference() {
    return new WeakActorInstanceStateTransition(new ActorInstanceStateTransition() {
      @Override
      public void transitionState(InstanceState desiredState) {
        stateMachine.postStateTransition(desiredState);
      }

      @Override
      public String getName() {
        return name;
      }
    });
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
                                 final ReactiveStateUpdater<InstanceState, ActorInstanceTransitionContext> updater,
                                 final Optional<ActorInstanceTransitionContext> transitionContext) {
    final ZonedDateTime now = ZonedDateTime.now().now();

    this.actorContext = new ActorContextImpl(context);

    CompletableFuture.anyOf(context.runAsync(() -> createEnclosedActor()))
        .whenComplete((value, throwable) -> {
          publishActionEvent(ActorActionEventType.CREATE_INSTANCE, now,
              determineOutcome(throwable));
          transitionConditionallyOnException((Throwable) throwable,
              InstanceState.STARTING, InstanceState.CREATE_FAILED, updater);
        });
  }

  @SuppressWarnings( {"PMD.AvoidFinalLocalVariable"})
  private void startNewInstance(final InstanceState sourceState,
                                final InstanceState targetState,
                                final ReactiveStateUpdater<InstanceState, ActorInstanceTransitionContext> updater,
                                final Optional<ActorInstanceTransitionContext> transitionContext) {
    final ZonedDateTime now = ZonedDateTime.now().now();

    context.runAsync(() -> sendSignal(Signal.PRE_START))
        .whenComplete((value, throwable) -> {
          publishSignalEvent(ActorSignalType.SIGNAL_PRE_START, now, determineOutcome((Throwable) throwable));
          transitionConditionallyOnException((Throwable) throwable,
            Signal.PRE_START,
            InstanceState.RUNNING, InstanceState.START_FAILED, updater);
        });
  }

  @SuppressWarnings( {"PMD.AvoidFinalLocalVariable"})
  private void restartInstance(final InstanceState sourceState,
                                final InstanceState targetState,
                                final ReactiveStateUpdater<InstanceState, ActorInstanceTransitionContext> updater,
                                final Optional<ActorInstanceTransitionContext> transitionContext) {
    final ZonedDateTime now = ZonedDateTime.now().now();

    context.runAsync(() -> sendSignal(Signal.PRE_RESTART))
        .whenComplete((value, throwable) -> {
          publishSignalEvent(ActorSignalType.SIGNAL_PRE_RESTART, now, determineOutcome((Throwable) throwable));
          transitionConditionallyOnException((Throwable) throwable,
              Signal.PRE_RESTART,
              InstanceState.RUNNING, InstanceState.RESTART_FAILED, updater);
        });
  }

  private void executeSupervisionStrategy(final InstanceState sourceState,
                                          final InstanceState targetState,
                                          final ReactiveStateUpdater<InstanceState, ActorInstanceTransitionContext> updater,
                                          final Optional<ActorInstanceTransitionContext> transitionContext) {
    final ActorInstanceTransitionContext ctx = transitionContext
        .orElseThrow(() -> new IllegalArgumentException("Expected transaction context on executing supervision strategy for actor {}" + name));

    context.disableMessageDelivery();

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
                                                  final InstanceState onExceptionState,
                                                  final ReactiveStateUpdater<InstanceState, ActorInstanceTransitionContext> updater) {
    Optional.ofNullable(throwable).ifPresentOrElse(toThrow -> {
      log.info("Actor name {} caught exception while transition from state {}",
          name, stateMachine.getCurrentState(), throwable);

      stateMachine.postStateTransition(onExceptionState, ActorInstanceTransitionContext.builder()
          .throwable(toThrow)
          .build());
    }, () -> stateMachine.postStateTransition(onSuccessState));
  }

  private void transitionConditionallyOnException(final Throwable throwable,
                                                  final Signal signal,
                                                  final InstanceState onSuccessState,
                                                  final InstanceState onExceptionState,
                                                  final ReactiveStateUpdater<InstanceState, ActorInstanceTransitionContext> updater) {
    Optional.ofNullable(throwable).ifPresentOrElse(toThrow -> {
      log.info("Actor name {} caught exception while transition from state {}",
          name, stateMachine.getCurrentState(), throwable);

      stateMachine.postStateTransition(onExceptionState, ActorInstanceTransitionContext.builder()
          .throwable(toThrow)
          .signal(signal)
          .build());
    }, () -> stateMachine.postStateTransition(onSuccessState));
  }

  private void terminateInstance(final InstanceState sourceState, final InstanceState targetState,
                                 final ReactiveStateUpdater<InstanceState, ActorInstanceTransitionContext> updater,
                                 final Optional<ActorInstanceTransitionContext> transitionContext) {
    stateMachine.stop();
    monitoringSignalPublisher.close();
    monitoringActionPublisher.close();
    context.terminateProcessing();
  }

  private void stopInstance(final InstanceState sourceState, final InstanceState targetState,
                            final ReactiveStateUpdater<InstanceState, ActorInstanceTransitionContext> updater,
                            final Optional<ActorInstanceTransitionContext> transitionContext) {
    final ZonedDateTime now = ZonedDateTime.now().now();

    context.runAsync(() -> sendSignal(Signal.POST_STOP))
        .whenComplete((value, throwable) -> {
          publishSignalEvent(ActorSignalType.SIGNAL_POST_STOP, now, determineOutcome((Throwable) throwable));
          stateMachine.postStateTransition(InstanceState.STOPPED);
        });
  }

  private void enableMessageDelivery(final InstanceState sourceState,
                                     final InstanceState targetState,
                                     final ReactiveStateUpdater<InstanceState, ActorInstanceTransitionContext> updater,
                                     final Optional<ActorInstanceTransitionContext> transitionContext) {
    context.enableMessageDelivery();
  }

  private void noAction(final InstanceState fromState, final InstanceState toState,
                        final Optional<ActorInstanceTransitionContext> transitionContext) {
  }

  private void noAction(final InstanceState fromState, final InstanceState toState,
                        final ReactiveStateUpdater<InstanceState, ActorInstanceTransitionContext> updater,
                        final Optional<ActorInstanceTransitionContext> transitionContext) {
    updater.postStateTransition(toState, transitionContext.orElse(null));
  }

  private void shift(final InstanceState fromState, final InstanceState toState,
                     final ReactiveStateUpdater<InstanceState, ActorInstanceTransitionContext> updater,
                     final Optional<ActorInstanceTransitionContext> transitionContext) {
    updater.postStateTransition(toState, transitionContext.orElse(null));
  }

  private void publishActionEvent(ActorActionEventType eventType, ZonedDateTime beginAction, ActorOutcomeType outcome) {
    log.info("actor {} publish action event {} with outcome {} at {}",
        name, eventType, outcome, beginAction);

    if (monitoringActionPublisher.hasSubscribers()) {
      monitoringActionPublisher.submit(ActorActionEvent.builder()
          .action(eventType)
          .actorName(name)
          .duration(Duration.between(beginAction, ZonedDateTime.now()))
          .outcome(outcome)
          .build());
    }
  }

  private void publishSignalEvent(ActorSignalType eventType, ZonedDateTime beginAction, ActorOutcomeType outcome) {
    log.info("actor {} publish signal event {} with outcome {} at {}",
        name, eventType, outcome, beginAction);

    if (monitoringSignalPublisher.hasSubscribers()) {
      monitoringSignalPublisher.submit(ActorSignalEvent.builder()
          .signal(eventType)
          .actorName(name)
          .duration(Duration.between(beginAction, ZonedDateTime.now()))
          .outcome(outcome)
          .build());
    }
  }

  private ActorOutcomeType determineOutcome(final Throwable throwable) {
    if(throwable != null) {
      return ActorOutcomeType.FAILURE;
    }

    return ActorOutcomeType.SUCCESS;
  }
}