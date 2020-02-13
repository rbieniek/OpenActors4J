package io.openactors4j.core.common;

/**
 * Public base interface of an actor
 */
public interface Actor {
  /**
   * Pass the context around the actor into the actor
   *
   * @param context the context information
   */
  default void setup(ActorContext context) {
  }

  /**
   * Receive and handle the passed lifecycle signal
   *
   * @param signal
   */
  void receiveSignal(Signal signal);

}
