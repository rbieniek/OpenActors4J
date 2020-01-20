package io.openactors4j.core.impl.common;

import io.openactors4j.core.boot.ActorSystemBootstrapConfiguration;
import io.openactors4j.core.common.ActorSystem;
import io.openactors4j.core.common.ActorSystemBuilder;
import io.openactors4j.core.untyped.UntypedActor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ActorSystemBuilderImpl implements ActorSystemBuilder {
  private final ActorSystemBootstrapConfiguration bootstrapConfiguration;

  private String name = "actor-system";
  private BiFunction factory = null;
  private ExecutorService executorService = new ThreadPoolExecutor(bootstrapConfiguration.getMinimalDefaultThreadPoolSize(),
      bootstrapConfiguration.getMaximalDefaultThreadPoolSize(),
      bootstrapConfiguration.getKeepaliveTime(),
      bootstrapConfiguration.getTimeUnit(),
      new LinkedBlockingQueue<>());
  private Consumer<Throwable> unrecoverableErrorHandler = new LoggingUnrecoverableErrorHandler();

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
  public ActorSystemBuilder withExecutorService(final ExecutorService executorService) {
    this.executorService = executorService;

    return this;
  }

  @Override
  public ActorSystemBuilder withUnrecoverableErrorHandler(Consumer<Throwable> handler) {
    return this;
  }

  @Override
  public ActorSystem build() {
    final ActorSystemImpl actorSystem = new ActorSystemImpl(name, factory,
        executorService, unrecoverableErrorHandler);

    try {
      actorSystem.start();
    } catch (Throwable t) {
      unrecoverableErrorHandler.accept(t);

      throw t;
    }

    return actorSystem;
  }
}
