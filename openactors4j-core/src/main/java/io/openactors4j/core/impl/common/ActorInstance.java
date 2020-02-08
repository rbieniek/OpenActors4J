package io.openactors4j.core.impl.common;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PROTECTED;
import static lombok.AccessLevel.PUBLIC;


import io.openactors4j.core.common.DeathNote;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.impl.messaging.Message;
import io.openactors4j.core.impl.messaging.RoutingSlip;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class models the actual actor instance held in the tree of actors inside the
 * actor system
 */
@RequiredArgsConstructor(access = PROTECTED)
@Getter(PROTECTED)
@Slf4j
public abstract class ActorInstance<T> {

  private final ActorInstanceContext context;
  @Getter(PUBLIC)
  private final String name;
  private final SupervisionStrategyInternal supervisionStrategy;
  private final StartupMode startupMode;

  private final Map<String, ActorInstance> childActors = new ConcurrentHashMap<>();

  @Getter(PUBLIC)
  private InstanceState instanceState = InstanceState.NEW;

  private ActorStateTransitions stateMachine = ActorStateTransitions.newInstance()
      .addState(InstanceState.NEW, InstanceState.RUNNING, this::startNewInstance)
      .addState(InstanceState.DELAYED, InstanceState.RUNNING, this::startDelayedInstance)
      .addState(InstanceState.DELAYED, InstanceState.STOPPED, this::stopInstance)
      .addState(InstanceState.RESTARTING, InstanceState.STOPPED, this::stopInstance)
      .addState(InstanceState.RESTARTING_DELAYED, InstanceState.STOPPED, this::stopInstance)
      .addState(InstanceState.RUNNING, InstanceState.STOPPED, this::stopInstance)
      .addState(InstanceState.STARTING, InstanceState.STOPPED, this::stopInstance)
      .addState(InstanceState.RUNNING, InstanceState.SUSPENDED, this::suspendInstance);

  /**
   * Move the actor instance to a new state.
   */
  public void transitionState(final InstanceState desiredState) {
    if (instanceState != desiredState) {
      log.info("Transition actor {} from state {} to new state {}", name, instanceState, desiredState);
      instanceState = stateMachine.lookup(InstanceState.NEW, InstanceState.RUNNING)
          .orElseThrow(() -> new IllegalStateException("Cannot transition from state " + instanceState + " to state " + desiredState))
          .apply(desiredState);
    }
  }

  /**
   * Lookup an actor instance from the path component of a given {@link RoutingSlip}
   *
   * @param routingSlip
   * @return
   */
  public Optional<ActorInstance> lookupActorInstance(final RoutingSlip routingSlip) {
    return Optional.empty();
  }

  /**
   * Route the incoming message according to its path:
   * <ul>
   * <li>if the target routing slip has q child path, route the message to the child actor with the
   * name denoted by next path part. If no child actor with a matching name can be found, send the
   * message to the systems unreachable handler</li>
   * <li>if the message if targeted to this actor, enqueue the message into the mailbox</li>
   * </ul>
   * <p>
   * If the message is destined for this instance, the message is enqueued.
   * <b>Please note:</b> If the actor is created with delayed startup, the actor is scheduled for starting
   *
   * @param message the message to be routed
   */
  public void routeMessage(final Message<T> message) {
    final Optional<String> currentPart = message.getTarget().nextPathPart();

    currentPart.ifPresentOrElse(pathPath -> {
      ofNullable(childActors.get(pathPath))
          .ifPresentOrElse(child -> child.routeMessage(message),
              () -> context.undeliverableMessage(message));
    }, () -> {
      if (message.getPayload() instanceof DeathNote) {
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
          default:
            throw new IllegalStateException("Cannot handle current instance state " + instanceState);
        }

      }
    });
  }

  /**
   * handle the next message in the mailbox
   */
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public void handleNextMessage(final Message<T> message) {
    try {
      handleMessage(message);
    } catch (Exception e) {
      transitionState(supervisionStrategy.handleProcessingException(e, this, context));
    }
  }

  /**
   * Handle a message which is destined for this actor
   *
   * @param message the message to process;
   */
  protected abstract void handleMessage(final Message<T> message);

  /**
   *
   */
  protected abstract void startInstance();

  /**
   * Attempt to start a new actor.
   * <p>
   * Depending on the start mode, the instance creation is started immediately or delayed until the arrvial of the first message
   *
   * @param desiredState the desired state, ignored in this case
   * @return
   */
  @SuppressWarnings( {"PMD.UnusedFormalParameter", "PMD.AvoidFinalLocalVariable"})
  private InstanceState startNewInstance(final InstanceState desiredState) {
    final InstanceState resultState;

    switch (startupMode) {
      case DELAYED:
        resultState = InstanceState.DELAYED;
        break;
      case IMMEDIATE:

        resultState = InstanceState.STARTING;
        break;
      default:
        throw new IllegalStateException("Cannot handle startup mode " + startupMode);
    }

    return resultState;
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private InstanceState startDelayedInstance(final InstanceState desiredState) {

    return InstanceState.STARTING;
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private InstanceState stopInstance(final InstanceState desiredState) {

    return InstanceState.STOPPED;
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private InstanceState suspendInstance(final InstanceState desiredState) {

    return InstanceState.STOPPED;
  }
}
