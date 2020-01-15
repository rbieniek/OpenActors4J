package io.openactors4j.core.typed;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public interface TypedActorRef<T> {
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
   */
  void tell(T message);

  /**
   * Interact with the actor in a two-way messaging style
   *
   * @param message the message payload
   * @return a {@link CompletionStage} for asynchronously handling the response
   */
  <V> CompletionStage<V> ask(T message);

  /**
   * Interact with the actor in a two-way messaging style
   *
   * @param message the message payload
   * @param timeout cancel further waiting for a response if this timeout expires
   * @return a {@link CompletionStage} for asynchronously handling the response
   */
  <V> CompletionStage<V> ask(T message, Duration timeout);
}
