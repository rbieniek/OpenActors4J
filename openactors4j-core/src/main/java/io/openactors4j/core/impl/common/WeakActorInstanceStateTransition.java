package io.openactors4j.core.impl.common;

import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * Transitioon the state of an {@link ActorInstance} based on a {@link WeakReference} to that
 * instance.
 * <p>
 * In case, the enclosed instance has already been terminated and garbage collected, the transition
 * will not be executed. This class is used when passing the {@link ActorInstance} reference
 * to a {@link io.openactors4j.core.impl.system.SupervisionStrategyInternal} which
 * may execute the transition after some significant amount of time
 */
public class WeakActorInstanceStateTransition implements ActorInstanceStateTransition {
  private final WeakReference<ActorInstanceStateTransition> reference;

  public WeakActorInstanceStateTransition(final ActorInstanceStateTransition actorInstance) {
    reference = new WeakReference<>(actorInstance);
  }

  @Override
  public void transitionState(InstanceState desiredState) {
    Optional.ofNullable(reference.get())
        .ifPresent(instance -> instance.transitionState(desiredState));
  }

  @Override
  public String getName() {
    return Optional.ofNullable(reference.get())
        .map(instance -> instance.getName())
        .orElse(null);
  }

}
