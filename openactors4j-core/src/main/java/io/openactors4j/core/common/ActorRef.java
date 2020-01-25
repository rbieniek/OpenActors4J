package io.openactors4j.core.common;

public interface ActorRef {
  /**
   * Obtain the full name of the actor in the context of its hosting actor system
   *
   * @return the fully qualified actor name
   */
  String name();
}
