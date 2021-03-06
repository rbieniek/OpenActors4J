package io.openactors4j.core.untyped;

import io.openactors4j.core.common.Actor;
import io.openactors4j.core.common.Signal;

/**
 * Public interface of an untyped actor, an actor which can handle any kind of message due to the
 * message class being {@link Object}
 */
public interface UntypedActor extends Actor {
  /**
   * Reliably pass the lifecycle signal information into the actor.
   * In contrast to the at-most-once message delivery contract, signal delivery is guaranteed.
   * <p>
   * The default implementation calls signal handler methods defined in this interface.
   *
   * @param lifecycleSignal the lifecycle signal
   */
  default void receiveSignal(Signal lifecycleSignal) {
    switch (lifecycleSignal) {
      case PRE_START:
        onPreStart();
        break;
      case PRE_RESTART:
        onPreRestart();
        break;
      case POST_STOP:
        onPostStop();
        break;
      case TERMINATE:
        // Actor implementations will ever see this signal
        break;
    }
  }

  /**
   * Called from the default signal handler on delivery of the {@link Signal#PRE_START} signal
   * which in turn is sent by the actor system before the actor is actually started.
   * <p>
   * The default behavior is to call the {@link UntypedActor#onPreRestart()} method
   */
  default void onPreStart() {
    onPreRestart();
  }

  /**
   * Called from the default signal handler on delivery of the {@link Signal#PRE_RESTART} signal
   * which in turn is sent by the actor system before the actor is restarted by a restarting
   * supervision strategy.
   * <p>
   * The default behavior is to do nothing.
   */
  default void onPreRestart() {
  }

  /**
   * Called from the default signal handler on delivery of the {@link Signal#POST_STOP} signal
   * which in turn is sent by the actor system after the actor is actually stopped.
   * <p>
   * The default behavior is to do nothing.
   */
  default void onPostStop() {
  }

  /**
   * Handle an inbound message
   *
   * @param message the message payload
   */
  void receive(Object message);
}
