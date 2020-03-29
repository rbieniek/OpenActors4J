package io.openactors4j.core.impl.common;

/**
 * This interface exposes all relevant mechanics to transition an {@link ActorInstance} to a new
 * state
 */
public interface ActorInstanceStateTransition {
  /**
   * Move an {@link ActorInstance} to a
   *
   * @param desiredState the state to move the {@link ActorInstance} to
   */
  void transitionState(final InstanceState desiredState);

  /**
   * Get the name
   *
   * @return the name
   */
  String getName();
}
