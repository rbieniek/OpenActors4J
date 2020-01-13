package io.openactors4j.core.common;

import io.openactors4j.core.typed.ActorBehavior;
import io.openactors4j.core.typed.TypedActorRef;
import io.openactors4j.core.untyped.UntypedActor;
import io.openactors4j.core.untyped.UntypedActorRef;
import java.util.Optional;

public interface ActorContext {
  <T, V, C extends ActorBehavior<T>> TypedActorRef<T, V> typedActor(Class<C> actorClass, String name, Object... params);

  <C extends UntypedActor> UntypedActorRef untypedActor(Class<C> actorClass, String name, Object... params);

  <T, V> Optional<TypedActorRef<T, V>> lookupTypedActor(String name);

  Optional<UntypedActorRef> lookupUntypedActor(String name);

  UntypedActorRef sender();
}
