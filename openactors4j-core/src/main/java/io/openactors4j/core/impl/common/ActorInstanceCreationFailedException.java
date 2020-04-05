package io.openactors4j.core.impl.common;

public class ActorInstanceCreationFailedException extends RuntimeException {
  private static final long serialVersionUID = 202003200100L;

  public ActorInstanceCreationFailedException(final Exception cause) {
    super(cause);
  }
}
