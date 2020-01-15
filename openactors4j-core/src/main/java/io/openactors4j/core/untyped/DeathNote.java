package io.openactors4j.core.untyped;

/**
 * Sending this message to an untyped actor will cause the actor to terminate and destroy itself
 */
@SuppressWarnings("PMD.ClassNamingConventions")
public class DeathNote {
  public static final DeathNote INSTANCE = new DeathNote();
}
