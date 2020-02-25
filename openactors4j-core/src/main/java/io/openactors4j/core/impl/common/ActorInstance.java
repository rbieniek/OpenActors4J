package io.openactors4j.core.impl.common;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PROTECTED;
import static lombok.AccessLevel.PUBLIC;

import io.openactors4j.core.common.Actor;
import io.openactors4j.core.common.DeathNote;
import io.openactors4j.core.common.Signal;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.impl.messaging.Message;
import io.openactors4j.core.impl.messaging.RoutingSlip;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * This class models the actual actor instance held in the tree of actors inside the
 * actor system.
 */
@RequiredArgsConstructor(access = PROTECTED)
@Getter(PROTECTED)
@Slf4j
@SuppressWarnings( {"PMD.TooManyMethods", "PMD.UnusedFormalParameter"})
public abstract class ActorInstance<V extends Actor, T> {

  private final ActorInstanceContext context;
  private final Callable<V> instanceSupplier;
  @Getter(PUBLIC)
  private final String name;
  private final SupervisionStrategyInternal supervisionStrategy;
  private final StartupMode startupMode;

  private final Map<String, ActorInstance> childActors = new ConcurrentHashMap<>();

  @Getter(PUBLIC)
  private InstanceState instanceState = InstanceState.NEW;

  @Getter(PROTECTED)
  private V instance;

  @Getter(PROTECTED)
  private ActorContextImpl actorContext;

  private ActorStateTransitions stateMachine = ActorStateTransitions.newInstance()
      .addState(InstanceState.NEW, InstanceState.RUNNING, this::startNewInstance)
      .addState(InstanceState.DELAYED, InstanceState.RUNNING, this::startDelayedInstance)
      .addState(InstanceState.DELAYED, InstanceState.STOPPED, this::stopInstance)
      .addState(InstanceState.RESTARTING, InstanceState.STOPPED, this::stopInstance)
      .addState(InstanceState.RESTARTING_DELAYED, InstanceState.STOPPED, this::stopInstance)
      .addState(InstanceState.RUNNING, InstanceState.STOPPED, this::stopInstance)
      .addState(InstanceState.STARTING, InstanceState.STOPPED, this::stopInstance)
      .addState(InstanceState.RUNNING, InstanceState.SUSPENDED, this::suspendInstance)
      .addState(InstanceState.STARTING, InstanceState.RUNNING, this::startComplete)
      .addState(InstanceState.STARTING, InstanceState.RESTARTING_DELAYED, this::startFailed)
      .addState(InstanceState.RUNNING, InstanceState.RESTARTING, this::restartInstance)
      .addState(InstanceState.RESTARTING, InstanceState.RUNNING, this::startComplete)
      ;

  /**
   * Move the actor instance to a new state.
   */
  public void transitionState(final InstanceState desiredState) {
    if (instanceState != desiredState) {
      log.info("Transition actor {} from state {} to new state {}",
          name,
          instanceState,
          desiredState);
      stateMachine.lookup(instanceState, desiredState)
          .orElseThrow(() -> new IllegalStateException("Cannot transition from state "
              + instanceState
              + " to state "
              + desiredState))
          .apply(desiredState)
          .ifPresent(state -> instanceState = state);
    }
  }

  /**
   * Lookup an actor instance from the path component of a given {@link RoutingSlip}.
   *
   * @param routingSlip message routing slip
   * @return an {@link Optional} to an {@link ActorInstance} i
   */
  public Optional<ActorInstance> lookupActorInstance(final RoutingSlip routingSlip) {
    return Optional.empty();
  }

  /**
   * Route the incoming message according to its path:
   *
   * <ul>
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

    currentPart.ifPresentOrElse(pathPath -> {
      ofNullable(childActors.get(pathPath))
          .ifPresentOrElse(child -> child.routeMessage(message),
              () -> {
                log.info("Actor {} undeliverable message to {}", name, pathPath);

                context.undeliverableMessage(message);
              });
    }, () -> {
      if (message.getPayload() instanceof DeathNote) {
        log.info("Actor {} received death note", name);

        transitionState(InstanceState.STOPPED);
      } else {
        context.enqueueMessage(message);

        switch (instanceState) {
          case RUNNING:
            context.scheduleMessageProcessing();
            break;
          case DELAYED:
            transitionState(InstanceState.RUNNING);
            break;
          case STARTING:
            // No further action required
            break;
          default:
            throw new IllegalStateException("Cannot handle current instance state "
                + instanceState);
        }

      }
    });
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

      transitionState(supervisionStrategy.handleProcessingException(e, this, context));
    } finally {
      actorContext.setCurrentSender(null);
    }

  }

  /**
   * Handle a message which is destined for this actor:
   *
   * @param message the message to process;
   */
  protected abstract void handleMessage(final Message<T> message) throws Exception;

  /**
   * Create the actor implementation instance:
   */
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  @SneakyThrows
  private void createInstance() {
    try {
      this.instance = instanceSupplier.call();
      this.instance.setupContext(this.actorContext);
    } catch (Exception e) {
      log.info("Caught exception on actor {} instance creation", name, e);

      throw e;
    }
  }

  /**
   * Handle signal reception
   */
  protected abstract void sendSignal(Signal signal) throws RuntimeException;

  /**
   * Attempt to start a new actor.
   *
   * <p>
   * Depending on the start mode, the instance creation is started
   * immediately or delayed until the arrvial of the first message:
   *
   * @param desiredState the desired state, ignored in this case
   * @return
   */
  @SuppressWarnings( {"PMD.AvoidFinalLocalVariable"})
  private Optional<InstanceState> startNewInstance(final InstanceState desiredState) {
    final InstanceState resultState;

    this.actorContext = new ActorContextImpl(context);

    switch (startupMode) {
      case DELAYED:
        resultState = InstanceState.DELAYED;
        break;
      case IMMEDIATE:
        instanceState = InstanceState.STARTING;

        context.runAsync(this::createInstance)
            .handle((s, t) -> sendPreStartSignalAfterCreation(s, (Throwable) t))
            .handle((s, t) -> decideStateAfterInstanceStart((Throwable) t))
            .whenComplete((state, throwable) -> transitionState((InstanceState) state));

        resultState = null;
        break;
      default:
        throw new IllegalStateException("Cannot handle startup mode " + startupMode);
    }

    return ofNullable(resultState);
  }

  @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
  private InstanceState decideStateAfterInstanceStart(final Throwable throwable) {
    InstanceState result = InstanceState.RUNNING;

    if (throwable != null) {
      result = InstanceState.RESTARTING_DELAYED;
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

  private Optional<InstanceState> startDelayedInstance(final InstanceState desiredState) {
    instanceState = InstanceState.STARTING;

    context.runAsync(this::createInstance)
        .handle((s, t) -> sendPreStartSignalAfterCreation(s, (Throwable) t))
        .handle((s, t) -> decideStateAfterInstanceStart((Throwable) t))
        .whenComplete((state, throwable) -> transitionState((InstanceState) state));

    return Optional.empty();
  }

  private Optional<InstanceState> stopInstance(final InstanceState desiredState) {

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
        .handle((s, t) -> decideStateAfterInstanceStart((Throwable) t))
        .whenComplete((state, throwable) -> transitionState((InstanceState) state));
    return empty();
  }
}
