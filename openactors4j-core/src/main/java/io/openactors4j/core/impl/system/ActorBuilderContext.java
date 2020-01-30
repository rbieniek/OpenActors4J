package io.openactors4j.core.impl.system;

import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.SupervisionStrategy;
import io.openactors4j.core.untyped.UntypedActor;
import io.openactors4j.core.untyped.UntypedActorRef;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Internal interface to provide contextual information held by the actor system to the
 * (untyped) actor builder implementation without fully exposing the actor system to the
 * builder implementation
 */
public interface ActorBuilderContext {
  /**
   * Obtain the default actor instance factory configured in the surrounding actor system
   *
   * @return the actor system-wide default
   */
  BiFunction<Class<? extends UntypedActor>, Object[], UntypedActor> defaultInstanceFactory();

  /**
   * Spawn a new untyped actor in the actor system
   *
   * @param name
   * @param supplier
   * @param mailbox
   * @param supervisionStrategy
   * @return
   */
  public UntypedActorRef spawnUntypedActor(String name, Supplier<? extends UntypedActor> supplier,
                                           Optional<Mailbox> mailbox, Optional<SupervisionStrategy> supervisionStrategy);

  boolean haveSiblingWithName(String name);
}
