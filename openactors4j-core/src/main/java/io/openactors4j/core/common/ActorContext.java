package io.openactors4j.core.common;

import io.openactors4j.core.typed.Behavior;
import io.openactors4j.core.typed.BehaviorBuilder;
import io.openactors4j.core.typed.TypedActorRef;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import io.openactors4j.core.untyped.UntypedActorRef;
import java.util.Optional;

public interface ActorContext {
  <T> BehaviorBuilder<T> newBehaviorBuilder();

  <T> TypedActorRef<T> spawn(Behavior<T> behavior, String name);

  UntypedActorBuilder spawnUntypedActor();

  <T> Optional<TypedActorRef<T>> lookupTypedActor(String name);

  Optional<UntypedActorRef> lookupUntypedActor(String name);

  UntypedActorRef sender();
}
