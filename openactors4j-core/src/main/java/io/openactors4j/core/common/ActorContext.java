package io.openactors4j.core.common;

import io.openactors4j.core.typed.Behavior;
import io.openactors4j.core.typed.BehaviorBuilder;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import java.util.Optional;

public interface ActorContext {
  <T> BehaviorBuilder<T> newBehaviorBuilder();

  UntypedActorBuilder newUntypedActorBuilder();

  <T> ActorRef<T> spawn(Behavior<T> behavior, String name);

  <T> Optional<ActorRef<T>> lookupActor(String name);

  ActorRef sender();
}
