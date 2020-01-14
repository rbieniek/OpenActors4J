package io.openactors4j.core.common;

import io.openactors4j.core.typed.ActorBehavior;
import io.openactors4j.core.typed.TypedActorRef;
import io.openactors4j.core.untyped.UntypedActor;
import io.openactors4j.core.untyped.UntypedActorRef;

public interface ActorSystem {
  String name();

  SystemAddress adress();

  <T, C extends ActorBehavior<T>> TypedActorRef<T> spawn(ActorBehavior<T> behavior, String name);

  <C extends UntypedActor> UntypedActorRef spawn(Class<C> actorClass, String name, Object... params);

}
