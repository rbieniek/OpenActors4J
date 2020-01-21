package io.openactors4j.core.common;

/**
 * This exception is raised in the context of the provided untyped actor factory in case
 * the specified combination of actor instacne class and constructor arguments.
 */
public class ActorInstantiationFailureException extends RuntimeException {
  private static final int serialVersionUID = 2020012101;

  public ActorInstantiationFailureException() {
    super();
  }

  public ActorInstantiationFailureException(final Throwable reason) {
    super(reason);
  }
}
