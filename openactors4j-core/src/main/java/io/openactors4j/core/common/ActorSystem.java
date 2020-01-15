package io.openactors4j.core.common;

import io.openactors4j.core.typed.ActorBehavior;
import io.openactors4j.core.typed.TypedActorRef;
import io.openactors4j.core.untyped.UntypedActorBuilder;

public interface ActorSystem {
  String name();

  SystemAddress adress();

  <T, C extends ActorBehavior<T>> TypedActorRef<T> spawn(ActorBehavior<T> behavior, String name);

  UntypedActorBuilder newUntypedActor();

}
