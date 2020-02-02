package io.openactors4j.core.untyped;

import io.openactors4j.core.common.ActorRef;
import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.common.SupervisionStrategy;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Builder interface used to create (from an {@link io.openactors4j.core.common.ActorSystem})
 * or spawn (from an {@link io.openactors4j.core.common.ActorContext} a new actor}
 * <p>
 * Either an actor class or an actor instance must be set. Omitting both or providing both will
 * fail the actor creating via the {@link UntypedActorBuilder#create()} builder method
 */
public interface UntypedActorBuilder {
  /**
   * Pass the actor implementation class to the Builder
   *
   * @param actorClass the implementation class
   * @param <T>        type parameter to constrain the implementation to be an
   *                   instance of {@link UntypedActor}
   * @return this builder instance
   */
  <T extends UntypedActor> UntypedActorBuilder withActorClass(Class<T> actorClass);

  /**
   * Pass a pre-allocated actor instance to the builder.
   *
   * @param supplier producing a pre-allocated actor instance
   * @param <T>      type parameter to constrain the implementation to be an
   *                 instance of {@link UntypedActor}
   * @return this build instance
   */
  <T extends UntypedActor> UntypedActorBuilder withSupplier(Supplier<T> supplier);

  /**
   * Pass a factory to be used for instance creation. Setting the factory instance her overrides
   * the actor system-wide factory set with
   * {@link io.openactors4j.core.common.ActorSystemBuilder#withUntypedActorFactory(BiFunction)}
   *
   * @param factory the factory instance
   * @param <T>     type parameter to constrain the implementation to be an
   *                instance of {@link UntypedActor}
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
   * <p>
   * This strategy is not executed when an {@link Error} is raised. The abnormal condition signaled
   * by the error can normally not coped with anyway, other than simply terminating the program
   * anyway.
   *
   * @param strategy the supervision strategy to be used
   * @return this builder instance
   */
  UntypedActorBuilder withSupervisionStrategy(SupervisionStrategy strategy);

  /**
   * The {@link Mailbox} to be used for this actor.
   * <p>
   * If this method is not used on the builder, it defaults
   * to {@link io.openactors4j.core.common.UnboundedMailbox}
   * </p>
   *
   * @param mailbox the {@link Mailbox} implementation to be used
   * @return this builder instance
   */
  UntypedActorBuilder withMailbox(Mailbox<Object> mailbox);

  /**
   * Set the name of the actor to be created.
   *
   * <B>Please note:</B> This name is absolute and trying to create a actor with a name which
   * already exists in the current scope, either {@link io.openactors4j.core.common.ActorSystem})
   * or {@link io.openactors4j.core.common.ActorContext}, will cause the creation to fail
   *
   * @param actorName the actor name
   * @return this builder instance
   */
  UntypedActorBuilder withAbsoluteName(String actorName);

  /**
   * Set the name prefix for the created actor.
   * <p>
   * The actor name will be constructed from this prefix and an unique numeric ID value
   * </p>
   *
   * @param actorNamePrefix the prefix part of the unique actor name
   * @return this builder instance
   */
  UntypedActorBuilder withNamePrefix(String actorNamePrefix);

  /**
   * Set the {@link StartupMode} for the actor.
   * <p>
   * The default behavior is to start the actor immediately on creation by using the default
   * value {@link StartupMode#IMMEDIATE}
   * </p>
   *
   * @param startupMode
   * @return this builder instance
   */
  UntypedActorBuilder withStartupMode(StartupMode startupMode);

  /**
   * Create the actor instance based upon the parametrization given by this build instance
   *
   * @return a reference to the created actor
   */
  ActorRef create();
}
