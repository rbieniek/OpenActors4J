package io.openactors4j.core.impl.common;

import io.openactors4j.core.impl.messaging.Message;

/**
 * Internal interface to provide contextual information and lifecycle methods
 * held by the actor system to the
 * actor instance implementation without fully exposing the actor system
 */
public interface ActorInstanceContext<T> {
  /**
   * Ask the actor system to schedule the calling actor instance for processing the next message
   * in the mailbox
   */
  void scheduleMessageProcessing();

  /**
   * Ask the actor system to schedule the calling actor instance for processing the next message
   * in the mailbox
   */
  void enqueueMessage(Message<T> message);

  /**
   * The calling actor implementation needs to give an unrouteable message back to the actor system
   *
   * @param message the message with routing slips and message payload
   */
  void undeliverableMessage(Message<T> message);

  /**
   * Retrieve the parent actor
   *
   * @return the parent actor instance
   */
  ActorInstance parentActor();
}
