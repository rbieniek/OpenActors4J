package io.openactors4j.core.common;

import io.openactors4j.core.typed.Behavior;
import io.openactors4j.core.typed.Behaviors;
import io.openactors4j.core.typed.TypedActorRef;
import io.openactors4j.core.untyped.UntypedActorBuilder;

public interface ActorSystem {
  String name();

  SystemAddress[] adress();

  <T> Behaviors behaviors();

  <T, C extends Behavior<T>> TypedActorRef<T> spawn(Behavior<T> behavior, String name);

  UntypedActorBuilder newUntypedActor();

  void shutown();
}
