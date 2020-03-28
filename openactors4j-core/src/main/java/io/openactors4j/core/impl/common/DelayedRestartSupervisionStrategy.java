package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DelayedRestartSupervisionStrategy implements SupervisionStrategyInternal {
  private static Map<Signal, InstanceState> SIGNAL_STATE_MAP = new ConcurrentHashMap<>();

  static {
    SIGNAL_STATE_MAP.put(Signal.PRE_START, InstanceState.STARTING);
    SIGNAL_STATE_MAP.put(Signal.PRE_RESTART, InstanceState.RESTARTING);
  }

  private final int maxRestarts;

  private final Duration restartPeriod;

  private final Optional<Duration> backoffPeriod;

  private final Optional<Integer> backoffFactor;

  private final ExecutorService timerExecutorService;

  private AtomicInteger retryCounter = new AtomicInteger(0);

  private AtomicInteger currentBackoffFactor = new AtomicInteger(0);

  @Override
  public void handleMessageProcessingException(final Exception processingException,
                                               final ActorInstanceStateTransition transition,
                                               final ActorInstanceContext context) {
    handleExceptionInternal(transition, InstanceState.RESTARTING);
  }

  @Override
  public void handleSignalProcessingException(final Throwable signalThrowable,
                                              final Signal signal,
                                              final ActorInstanceStateTransition transition,
                                              final ActorInstanceContext context) {
    handleExceptionInternal(transition, determineStateFromSignal(signal));
  }

  @Override
  public void handleActorCreationException(final Throwable signalThrowable,
                                           final ActorInstanceStateTransition transition,
                                           final ActorInstanceContext context) {
    handleExceptionInternal(transition, InstanceState.CREATING);
  }

  private void handleExceptionInternal(final ActorInstanceStateTransition transition,
                                       final InstanceState wakeupState) {
    final Optional<InstanceState> instanceState = incrementAndCheckRetryCounter(Optional.of(maxRestarts)
        .filter(value -> value > 0));

    instanceState.ifPresentOrElse(state -> transition.transitionState(state),
        () -> CompletableFuture.delayedExecutor(calculateRestartPeriod().toMillis(),
            TimeUnit.MILLISECONDS,
            timerExecutorService).execute(() -> {
          try {
            transition.transitionState(wakeupState);
          } catch (final IllegalStateException ise) {
            log.warn("Cannot execute state transition on actor {}", transition.getName(), ise);
          }
        }));
  }

  private Optional<InstanceState> incrementAndCheckRetryCounter(final Optional<Integer> maxRetries) {
    return maxRetries
        .filter(value -> value < retryCounter.incrementAndGet())
        .map(value -> InstanceState.STOPPED);
  }

  private Duration calculateRestartPeriod() {
    return restartPeriod.plus(backoffPeriod
        .map(duration -> duration.multipliedBy(currentBackoffFactor
            .getAndAdd(backoffFactor.orElse(1))))
        .orElse(Duration.ZERO));
  }


  private InstanceState determineStateFromSignal(final Signal signal) {
    return Optional.ofNullable(SIGNAL_STATE_MAP.get(signal)).orElse(InstanceState.STOPPED);
  }
}
