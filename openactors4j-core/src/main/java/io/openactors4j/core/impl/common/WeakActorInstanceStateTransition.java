package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Actor;
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
 *
 * @param <V> type reference to enclosed actor
 * @param <T> actor result type
 */
public class WeakActorInstanceStateTransition<V extends Actor, T> implements ActorInstanceStateTransition {
  private WeakReference<ActorInstance<V, T>> reference;

  public WeakActorInstanceStateTransition(final ActorInstance<V, T> actorInstance) {
    reference = new WeakReference<>(actorInstance);
  }

  @Override
  public void transitionState(InstanceState desiredState) {
    Optional.ofNullable(reference.get())
        .ifPresent(instance -> instance.transitionState(desiredState));
  }
}
