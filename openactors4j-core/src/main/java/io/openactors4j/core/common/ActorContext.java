package io.openactors4j.core.common;

import io.openactors4j.core.typed.ActorBehavior;
import io.openactors4j.core.typed.TypedActorRef;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import io.openactors4j.core.untyped.UntypedActorRef;
import java.util.Optional;

public interface ActorContext {
  <T, C extends ActorBehavior<T>> TypedActorRef<T> spawn(ActorBehavior<T> behavior, String name);

  UntypedActorBuilder spawnUntypedActor();

  <T> Optional<TypedActorRef<T>> lookupTypedActor(String name);

  Optional<UntypedActorRef> lookupUntypedActor(String name);

  UntypedActorRef sender();
}
