package io.openactors4j.core.impl.system;

import io.openactors4j.core.common.ActorSystem;
import io.openactors4j.core.common.SystemAddress;
import io.openactors4j.core.common.ThreadPoolConfiguration;
import io.openactors4j.core.common.TimerThreadPoolConfiguration;
import io.openactors4j.core.impl.common.ActorBuilderContext;
import io.openactors4j.core.impl.messaging.SystemAddressImpl;
import io.openactors4j.core.impl.spi.MessageContextManagement;
import io.openactors4j.core.impl.untyped.UntypedActorBuilderImpl;
import io.openactors4j.core.typed.Behavior;
import io.openactors4j.core.typed.Behaviors;
import io.openactors4j.core.typed.TypedActorRef;
import io.openactors4j.core.untyped.UntypedActor;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ActorSystemImpl implements ActorSystem, Closeable {

  private final String systemName;
  private final BiFunction<Class<? extends UntypedActor>, Object[], UntypedActor> factory;
  @Getter
  private final ThreadPoolConfiguration userThreadPoolConfiguration;
  @Getter
  private final ThreadPoolConfiguration systemThreadPoolConfiguration;
  @Getter
  private final TimerThreadPoolConfiguration timerThreadPoolConfiguration;
  private final Consumer<Throwable> unrecoverableErrorHandler;

  private ExecutorService userExecutorService;
  private ExecutorService systemExecutorService;
  private ScheduledExecutorService timerExecutorService;
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
    return new UntypedActorBuilderImpl(new ActorBuilderContext() {
      @Override
      public BiFunction<Class<? extends UntypedActor>, Object[], UntypedActor> defaultInstanceFactory() {
        return factory;
      }

      @Override
      public SystemAddress actorAddress(String name) {
        return null;
      }

      @Override
      public boolean haveSiblingWithName(String name) {
        return false;
      }
    });
  }

  @SuppressWarnings("PMD.DefaultPackage")
    /* default */ void start(final List<MessageContextManagement> contextManagements) {
    log.info("Starting actor system {} with threadpools user {} and system {}",
        systemName,
        userThreadPoolConfiguration,
        systemThreadPoolConfiguration);

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

    timerExecutorService = buildTimerScheduler();

    this.contextManagements.addAll(contextManagements);

    systemAddresses.add(SystemAddressImpl.builder()
        .hostname("localhost")
        .systemName(systemName)
        .transportScheme("local")
        .build());

    log.info("Started actor system {} with bindings {}",
        systemName,
        systemAddresses.stream()
            .map(sa -> sa.transport().toString())
            .reduce((a, b) -> String.format("%s,%s", a, b))
            .get());
  }

  @Override
  public void shutown() {
    // TODO: Handle list of returned runnables, log what was abourted
    timerExecutorService.shutdownNow();

    // TODO: Handle list of returned runnables, log what was abourted
    userExecutorService.shutdownNow();

    // TODO: Handle list of returned runnables, log what was abourted
    systemExecutorService.shutdownNow();
  }

  @Override
  public void close() {
    shutown();
  }

  private ScheduledThreadPoolExecutor buildTimerScheduler() {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(timerThreadPoolConfiguration.getCorePoolSize());

    executor.setRemoveOnCancelPolicy(true);
    executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

    return executor;
  }
}
