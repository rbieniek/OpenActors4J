package io.openactors4j.core.impl.common;

import io.openactors4j.core.impl.messaging.Message;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

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
   * Assign the actor instance to this context object and start it
   */
  void assignAndStart(ActorInstance<T> actorInstance);

  /**
   * Submit a runnable to be executed in a threadpool provided by the actor system
   *
   * @param runnable the task to be executed
   * @return a {@link CompletionStage} for handling further processing after the task has been scheduled
   */
  CompletionStage<Void> runAsync(Runnable runnable);

  /**
   * Submit a supplier to be executed in a threadpool provided by the actor system
   *
   * @param supplier the task to be executed
   * @return a {@link CompletionStage} for handling further processing after the task has been scheduled
   */
  <V> CompletionStage<V> submitAsync(Supplier<V> supplier);

  /**
   * Retrieve the parent actor
   *
   * @return the parent actor instance
   */
  ActorInstance parentActor();
}
