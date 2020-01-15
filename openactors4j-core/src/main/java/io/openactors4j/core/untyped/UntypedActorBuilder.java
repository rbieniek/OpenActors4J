package io.openactors4j.core.untyped;

import io.openactors4j.core.common.SupervisionStrategy;
import java.util.function.BiFunction;

/**
 * Builder interface used to create (from an {@link io.openactors4j.core.common.ActorSystem})
 * or spawn (from an {@link io.openactors4j.core.common.ActorContext] a new actor}
 */
public interface UntypedActorBuilder {
  /**
   * Pass the actor implementation class to the Builder
   *
   * @param actorClass the implementation class
   * @param <T> type parameter to constrain the implementation to be an
   *           instance of {@link UntypedActor}
   * @return this builder instance
   */
  <T extends UntypedActor> UntypedActorBuilder withActorClass(Class<T> actorClass);

  /**
   * Pass a factory to be used for instance creation. Setting the factory instance her overrides
   * the actor system-wide factory set with
   * {@link io.openactors4j.core.common.ActorSystemBuilder#withUntypedActorFactory(BiFunction)}
   *
   * @param factory the factory instance
   * @param <T> type parameter to constrain the implementation to be an
   *           instance of {@link UntypedActor}
   * @return this builder instance
   */
  <T extends UntypedActor> UntypedActorBuilder withFactory(BiFunction<Class<T>, Object[], T> factory);

  /**
   * Provide arguments to the instance constructor
   *
   * @param arguments any number of arguments passed transparently to the instance constructor
   * @return this builder instance
   */
  UntypedActorBuilder withArguments(Object... arguments);

  /**
   * The {@link SupervisionStrategy} to be executed on the created instance if the message
   * processing raises an {@link Exception}.
   *
   * This strategy is not executed when an {@link Error} is raised. The abnormal condition signaled
   * by the error can normally not coped with anyway, other than simply terminating the program
   * anyway.
   *
   * @param strategy
   * @return this builder instance
   */
  UntypedActorBuilder withSupervisionStrategy(SupervisionStrategy strategy);

  /**
   * Set the name of the actor to be created.
   *
   * <B></B>Please note:</B> This name is absolute and trying to create a actor with a name which
   * already exists in the current scope, either {@link io.openactors4j.core.common.ActorSystem})
   * or {@link io.openactors4j.core.common.ActorContext], will cause the creation to fail
   *
   * @param actorName the actor name
   * @return this builder instance
   */
  UntypedActorBuilder withAbsoluteName(String actorName);

  /**
   * Set the name prefix for the created actor.
   *
   * The actor name will be constructed from this prefix and an unique numeric ID value
   *
   * @param actorNamePrefix
   * @return this builder instance
   */
  UntypedActorBuilder withNamePrefix(String actorNamePrefix);

  /**
   * Create the actor instance based upon the parametrization given by this build instance
   *
   * @return a reference to the created actor
   */
  UntypedActorRef create();
}
