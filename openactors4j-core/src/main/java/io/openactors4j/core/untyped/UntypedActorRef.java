package io.openactors4j.core.untyped;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Handle for accessing an actor
 */
public interface UntypedActorRef {
  /**
   * Obtain the full name of the actor in the context of its hosting actor system
   *
   * @return the fully qualified actor name
   */
  String name();

  /**
   * Interact with the actor in a one-way messaging style
   *
   * @param message the message payload
   * @return this actor reference, suitable for message chaining
   */
  UntypedActorRef tell(Object message);

  /**
   * Interact with the actor in a two-way messaging style
   *
   * @param message the message payload
   * @return a {@link CompletionStage} for asynchronously handling the response
   */
  CompletionStage<Object> ask(Object message);


  /**
   * Interact with the actor in a two-way messaging style
   *
   * @param message the message payload
   * @param timeout cancel further waiting for a response if this timeout expires
   * @return a {@link CompletionStage} for asynchronously handling the response
   */
  CompletionStage<Object> ask(Object message, TimeUnit timeout);
}
