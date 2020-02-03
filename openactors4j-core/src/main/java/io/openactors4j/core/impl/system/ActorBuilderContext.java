package io.openactors4j.core.impl.system;

import io.openactors4j.core.common.ActorRef;
import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.untyped.UntypedActor;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Internal interface to provide contextual information held by the actor system to the
 * (untyped) actor builder implementation without fully exposing the actor system to the
 * builder implementation
 */
public interface ActorBuilderContext<T> {
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
  public ActorRef spawnUntypedActor(String name, Supplier<? extends UntypedActor> supplier,
                                    Optional<Mailbox> mailbox, Optional<SupervisionStrategyInternal> supervisionStrategy,
                                    Optional<StartupMode> startupMode);

  /**
   * Determine if there is already a sibling actor existing with an equal name
   *
   * @param name the actor name to be checked
   * @return <b>true</b> if a sibling with the same name already exists, <b>false</b> otherwise
   */
  boolean haveSiblingWithName(String name);
}
