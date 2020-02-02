package io.openactors4j.core.common;

/**
 * This enumeration defines the various optionas about the time an actor is started.
 */
public enum StartupMode {
  /**
   * Start the actor immediately after the creation
   */
  IMMEDIATE,

  /**
   * Start the actor when the first message arrives
   */
  DELAYED;
}
