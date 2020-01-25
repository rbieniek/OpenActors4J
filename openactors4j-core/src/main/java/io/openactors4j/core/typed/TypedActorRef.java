package io.openactors4j.core.typed;

import io.openactors4j.core.common.ActorRef;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

public interface TypedActorRef<T> extends ActorRef {
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
   * @param <V>     Return object type
   * @return a {@link CompletionStage} for asynchronously handling the response
   */
  <V> CompletionStage<V> ask(T message);

  /**
   * Interact with the actor in a two-way messaging style
   *
   * @param message the message payload
   * @param timeout cancel further waiting for a response if this timeout expires
   * @param <V>     Return object type
   * @return a {@link CompletionStage} for asynchronously handling the response
   */
  <V> CompletionStage<V> ask(T message, Duration timeout);
}
