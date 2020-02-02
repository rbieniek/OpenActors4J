package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.ActorRef;
import io.openactors4j.core.typed.Behavior;
import io.openactors4j.core.typed.BehaviorBuilder;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import java.util.Optional;

public class ActorContextImpl implements ActorContext {
  @Override
  public <T> BehaviorBuilder<T> newBehaviorBuilder() {
    return null;
  }

  @Override
  public <T> ActorRef<T> spawn(final Behavior<T> behavior, final String name) {
    return null;
  }

  @Override
  public UntypedActorBuilder newUntypedActorBuilder() {
    return null;
  }

  @Override
  public <T> Optional<ActorRef<T>> lookupActor(final String name) {
    return Optional.empty();
  }

  @Override
  public ActorRef sender() {
    return null;
  }
}
