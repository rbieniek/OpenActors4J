package io.openactors4j.core.common;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public interface ActorRef<T> {
  /**
   * An actor reference that should be used if no reply message is expected to be sent by the targetted actor
   *
   * @return An actor reference pointing to a system handler in case there are replies
   * sent by the targetted actor. All methods in the actor reference will yield
   * a {@link NotImplementedException}
   */
  static ActorRef<?> noSender() {
    return new ActorRef() {

      @Override
      public String name() {
        return "/system/no-sender";
      }

      @Override
      public void tell(Object message) {
        throw new NotImplementedException();
      }

      @Override
      public void tell(Object message, ActorRef sender) {
        throw new NotImplementedException();
      }

      @Override
      public CompletionStage<Object> ask(Object message) {
        throw new NotImplementedException();
      }

      @Override
      public CompletionStage<Object> ask(Object message, Duration timeout) {
        throw new NotImplementedException();
      }
    };
  }

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
  default void tell(T message) {
    tell(message, ActorRef.noSender());
  }

  ;

  /**
   * Interact with the actor in a one-way messaging style.
   *
   * @param message the message payload
   * @param sender  the sender of the message
   */
  void tell(T message, ActorRef sender);

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
