package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.ActorSystem;
import io.openactors4j.core.common.SystemAddress;
import io.openactors4j.core.common.ThreadPoolConfiguration;
import io.openactors4j.core.typed.Behavior;
import io.openactors4j.core.typed.Behaviors;
import io.openactors4j.core.typed.TypedActorRef;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ActorSystemImpl implements ActorSystem, Closeable {

  private final String systemName;
  private final BiFunction factory;
  @Getter
  private final ThreadPoolConfiguration userThreadPoolConfiguration;
  @Getter
  private final ThreadPoolConfiguration systemThreadPoolConfiguration;
  private final Consumer<Throwable> unrecoverableErrorHandler;

  private ExecutorService userExecutorService;
  private ExecutorService systemExecutorService;

  @Override
  public String name() {
    return systemName;
  }

  @Override
  public SystemAddress[] adress() {
    return new SystemAddress[0];
  }

  @Override
  public <T> Behaviors behaviors() {
    return null;
  }

  @Override
  public <T, C extends Behavior<T>> TypedActorRef<T> spawn(final Behavior<T> behavior, final String name) {
    return null;
  }

  @Override
  public UntypedActorBuilder newUntypedActor() {
    return null;
  }

  @SuppressWarnings("PMD.DefaultPackage")
  /* default */ void start() {
    userExecutorService = new ThreadPoolExecutor(userThreadPoolConfiguration.getMinimalDefaultThreadPoolSize(),
        userThreadPoolConfiguration.getMaximalDefaultThreadPoolSize(),
        userThreadPoolConfiguration.getKeepaliveTime(),
        userThreadPoolConfiguration.getTimeUnit(),
        new LinkedBlockingQueue<>());

    systemExecutorService = new ThreadPoolExecutor(systemThreadPoolConfiguration.getMinimalDefaultThreadPoolSize(),
        systemThreadPoolConfiguration.getMaximalDefaultThreadPoolSize(),
        systemThreadPoolConfiguration.getKeepaliveTime(),
        systemThreadPoolConfiguration.getTimeUnit(),
        new LinkedBlockingQueue<>());
  }

  @Override
  public void shutown() {
    // TODO: Handle list of returned runnables, log what was abourted
    userExecutorService.shutdownNow();

    // TODO: Handle list of returned runnables, log what was abourted
    systemExecutorService.shutdownNow();
  }

  @Override
  public void close() {
    shutown();
  }
}