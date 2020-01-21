package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.ActorSystem;
import io.openactors4j.core.common.ActorSystemBuilder;
import io.openactors4j.core.common.ThreadPoolConfiguration;
import io.openactors4j.core.untyped.UntypedActor;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ActorSystemBuilderImpl implements ActorSystemBuilder {
  private String name = "actor-system";
  private BiFunction factory = new DefaultActorInstanceFactory();
  private ThreadPoolConfiguration userThreadPoolConfiguration = ThreadPoolConfiguration.builder().build();
  private ThreadPoolConfiguration systemThreadPoolConfiguration = ThreadPoolConfiguration.builder().build();
  private Consumer<Throwable> unrecoverableErrorHandler = new LoggingUnrecoverableErrorHandler();


  @Override
  public ActorSystemBuilder withUserThreadPoolConfiguration(final ThreadPoolConfiguration parameters) {
    this.userThreadPoolConfiguration = parameters;

    return this;
  }

  @Override
  public ActorSystemBuilder withSystemThreadPoolConfiguration(final ThreadPoolConfiguration parameters) {
    this.systemThreadPoolConfiguration = parameters;

    return this;
  }

  @Override
  public ActorSystemBuilder withName(final String name) {
    this.name = name;

    return this;
  }

  @Override
  public <T extends UntypedActor> ActorSystemBuilder withUntypedActorFactory(final BiFunction<Class<T>, Object[], T> factory) {
    this.factory = factory;

    return this;
  }

  @Override
  public ActorSystemBuilder withUnrecoverableErrorHandler(final Consumer<Throwable> handler) {
    this.unrecoverableErrorHandler = handler;

    return this;
  }

  @Override
  @SuppressWarnings("PMD.AvoidCatchingThrowable")
  public ActorSystem build() {
    final ActorSystemImpl actorSystem = new ActorSystemImpl(name, factory,
        userThreadPoolConfiguration, systemThreadPoolConfiguration, unrecoverableErrorHandler);

    try {
      actorSystem.start();
    } catch (Throwable t) {
      unrecoverableErrorHandler.accept(t);

      throw t;
    }

    return actorSystem;
  }
}
