package io.openactors4j.core.impl.system;

import io.openactors4j.core.common.Signal;
import io.openactors4j.core.common.SupervisionStrategy;
import io.openactors4j.core.impl.common.ActorInstance;
import io.openactors4j.core.impl.common.ActorInstanceContext;
import io.openactors4j.core.impl.common.ActorInstanceStateTransition;
import io.openactors4j.core.impl.common.InstanceState;
import java.util.Optional;

/**
 * Internal extension interface for modeling common functionality of supervision strategies
 * which are kept internal to the implementation layer
 */
public interface SupervisionStrategyInternal extends SupervisionStrategy {
  /**
   * Handle message-processing failure
   *
   * @param processingException the exception raised by the message handler
   * @param transition used to move into new state if necessary
   * @param context the actor instance context
   */
  void handleMessageProcessingException(Exception processingException, ActorInstanceStateTransition transition, ActorInstanceContext context);

  /**
   * Handle signal-processing failure
   *
   * @param signalThrowable the exception raised by the message handler
   * @param signal          the {@link Signal} which was failed by the actor
   * @param transition used to move into new state if necessary
   * @param context         the actor instance context
   */
  void handleSignalProcessingException(Throwable signalThrowable, Signal signal, ActorInstanceStateTransition transition, ActorInstanceContext context);

  /**
   * Handle instance creation failure
   *
   * @param signalThrowable the exception raised by the message handler
   * @param transition used to move into new state if necessary
   * @param context         the actor instance context
   */
  void handleActorCreationException(Throwable signalThrowable, ActorInstanceStateTransition transition, ActorInstanceContext context);

}
