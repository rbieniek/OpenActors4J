package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.SystemAddress;
import io.openactors4j.core.untyped.UntypedActor;
import java.util.function.BiFunction;

/**
 * Internal interface to provide contextual information held by the actor system to the
 * (untyped) actor builder implementation without fully exposing the actor system to the
 * builder implementation
 */
public interface ActorBuilderContext {
  BiFunction<Class<? extends UntypedActor>, Object[], UntypedActor> defaultInstanceFactory();

  SystemAddress actorAddress(String name);

  boolean haveSiblingWithName(String name);
}
