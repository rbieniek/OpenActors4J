package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.typed.Behavior;
import io.openactors4j.core.typed.BehaviorBuilder;
import io.openactors4j.core.typed.TypedActorRef;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import io.openactors4j.core.untyped.UntypedActorRef;
import java.util.Optional;

public class ActorContextImpl implements ActorContext {
  @Override
  public <T> BehaviorBuilder<T> newBehaviorBuilder() {
    return null;
  }

  @Override
  public <T> TypedActorRef<T> spawn(final Behavior<T> behavior, final String name) {
    return null;
  }

  @Override
  public UntypedActorBuilder spawnUntypedActor() {
    return null;
  }

  @Override
  public <T> Optional<TypedActorRef<T>> lookupTypedActor(final String name) {
    return Optional.empty();
  }

  @Override
  public Optional<UntypedActorRef> lookupUntypedActor(final String name) {
    return Optional.empty();
  }

  @Override
  public UntypedActorRef sender() {
    return null;
  }
}
