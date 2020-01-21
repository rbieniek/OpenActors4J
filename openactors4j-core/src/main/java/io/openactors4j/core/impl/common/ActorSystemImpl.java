package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.ActorSystem;
import io.openactors4j.core.common.SystemAddress;
import io.openactors4j.core.common.ThreadPoolConfiguration;
import io.openactors4j.core.impl.spi.MessageContextManagement;
import io.openactors4j.core.typed.Behavior;
import io.openactors4j.core.typed.Behaviors;
import io.openactors4j.core.typed.TypedActorRef;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
  private final Queue contextManagements = new ConcurrentLinkedQueue();
  private final List<SystemAddress> systemAddresses = new LinkedList<>();

  @Override
  public String name() {
    return systemName;
  }

  @Override
  public SystemAddress[] adress() {
    final ArrayList<SystemAddress> addresses = new ArrayList<>(systemAddresses);

    return addresses.toArray(new SystemAddress[0]);
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
    /* default */ void start(final List<MessageContextManagement> contextManagements) {
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

    this.contextManagements.addAll(contextManagements);

    systemAddresses.add(SystemAddressImpl.builder()
        .hostname("localhost")
        .systemName(systemName)
        .transportScheme("local")
        .build());
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
