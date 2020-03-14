package io.openactors4j.core.impl.system;

import io.openactors4j.core.common.Signal;
import io.openactors4j.core.common.SupervisionStrategy;
import io.openactors4j.core.impl.common.ActorInstance;
import io.openactors4j.core.impl.common.ActorInstanceContext;
import io.openactors4j.core.impl.common.InstanceState;

/**
 * Internal extension interface for modeling common functionality of supervision strategies
 * which are kept internal to the implementation layer
 */
public interface SupervisionStrategyInternal extends SupervisionStrategy {
  /**
   * Handle message-processing failure
   *
   * @param processingException the exception raised by the message handler
   * @param actorInstance       the actor instance holding the actor which failed the message processing
   * @param context             the actor instance context
   * @return the next state to transisition the instance to
   */
  InstanceState handleMessageProcessingException(Exception processingException, ActorInstance actorInstance, ActorInstanceContext context);

  /**
   * Handle signal-processing failure
   *
   * @param signalThrowable the exception raised by the message handler
   * @param signal          the {@link Signal} which was failed by the actor
   * @param actorInstance   the actor instance holding the actor which failed the signal processing
   * @param context         the actor instance context
   * @return the next state to transisition the instance to
   */
  InstanceState handleSignalProcessingException(Throwable signalThrowable, Signal signal, ActorInstance actorInstance, ActorInstanceContext context);
}
