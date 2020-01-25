package io.openactors4j.core.untyped;

import io.openactors4j.core.common.ActorRef;
import io.openactors4j.core.common.NotImplementedException;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * Handle for accessing an actor
 */
public interface UntypedActorRef extends ActorRef {
  /**
   * An actor reference that should be used if no reply message is expected to be sent by the targetted actor
   *
   * @return An actor reference pointing to a system handler in case there are replies
   * sent by the targetted actor. All methods in the actor reference will yield
   * a {@link NotImplementedException}
   */
  static UntypedActorRef noSender() {
    return new UntypedActorRef() {

      @Override
      public String name() {
        return "/system/no-sender";
      }

      @Override
      public UntypedActorRef tell(Object message, UntypedActorRef sender) {
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
   * Interact with the actor in a one-way messaging style
   *
   * @param message the message payload
   * @param sender  the sender of the message to whom the reply is sent
   * @return this actor reference, suitable for message chaining
   */
  UntypedActorRef tell(Object message, UntypedActorRef sender);

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
  CompletionStage<Object> ask(Object message, Duration timeout);
}
