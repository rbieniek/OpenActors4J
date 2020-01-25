package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.ActorSystem;
import io.openactors4j.core.common.ActorSystemBuilder;
import io.openactors4j.core.common.ThreadPoolConfiguration;
import io.openactors4j.core.common.TimerThreadPoolConfiguration;
import io.openactors4j.core.impl.spi.MessageContextManagement;
import io.openactors4j.core.spi.MessageContextManager;
import io.openactors4j.core.spi.MessageContextProvider;
import io.openactors4j.core.untyped.UntypedActor;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ActorSystemBuilderImpl implements ActorSystemBuilder {
  private String name = "actor-system";
  private BiFunction factory = new DefaultActorInstanceFactory();
  private ThreadPoolConfiguration userThreadPoolConfiguration = ThreadPoolConfiguration.builder().build();
  private ThreadPoolConfiguration systemThreadPoolConfiguration = ThreadPoolConfiguration.builder().build();
  private TimerThreadPoolConfiguration timerThreadPoolConfiguration = TimerThreadPoolConfiguration.builder().build();
  private Consumer<Throwable> unrecoverableErrorHandler = new LoggingUnrecoverableErrorHandler();
  private final List<MessageContextManagement> contextManagements = new LinkedList<>();

  @Override
  public ActorSystemBuilder withUserThreadPoolConfiguration(final ThreadPoolConfiguration parameters) {
    this.userThreadPoolConfiguration = parameters;

    return this;
  }

  @Override
  public ActorSystemBuilder withTimerThreadPoolConfiguration(final TimerThreadPoolConfiguration parameters) {
    this.timerThreadPoolConfiguration = parameters;

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
  public ActorSystemBuilder withMessageContextHandler(final MessageContextProvider provider, final MessageContextManager manager) {

    contextManagements.add(MessageContextManagement.builder()
        .manager(manager)
        .provider(provider)
        .build());

    return this;
  }

  @Override
  @SuppressWarnings("PMD.AvoidCatchingThrowable")
  public ActorSystem build() {

    try {
      final ActorSystemImpl actorSystem = new ActorSystemImpl(name, factory,
          userThreadPoolConfiguration, systemThreadPoolConfiguration, timerThreadPoolConfiguration,
          unrecoverableErrorHandler);


      actorSystem.start(contextManagements);

      return actorSystem;
    } catch (Throwable t) {
      unrecoverableErrorHandler.accept(t);

      throw t;
    }
  }
}
