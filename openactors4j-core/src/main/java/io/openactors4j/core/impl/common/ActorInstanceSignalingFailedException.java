package io.openactors4j.core.impl.common;

public class ActorInstanceSignalingFailedException extends RuntimeException {
  private static final long serialVersionUID = 202003200100L;

  public ActorInstanceSignalingFailedException(final Exception e) {
    super(e);
  }
}
