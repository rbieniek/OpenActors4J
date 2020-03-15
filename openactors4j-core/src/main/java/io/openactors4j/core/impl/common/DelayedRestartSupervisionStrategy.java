package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DelayedRestartSupervisionStrategy implements SupervisionStrategyInternal {

  private final int maxRestarts;

  private final Duration restartPeriod;

  private final Optional<Duration> backoffPeriod;

  private final Optional<Integer> backoffFactor;

  private final ExecutorService timerExecutorService;

  private AtomicInteger retryCounter = new AtomicInteger(0);

  private AtomicInteger currentBackoffFactor = new AtomicInteger(0);

  @Override
  public Optional<InstanceState> handleMessageProcessingException(final Exception processingException,
                                                                  final ActorInstance actorInstance,
                                                                  final ActorInstanceContext context) {
    return handleExceptionInternal(actorInstance, InstanceState.RESTARTING);
  }

  @Override
  public Optional<InstanceState> handleSignalProcessingException(final Throwable signalThrowable,
                                                                 final Signal signal,
                                                                 final ActorInstance actorInstance,
                                                                 final ActorInstanceContext context) {
    return handleExceptionInternal(actorInstance, determineStateFromSignal(signal));
  }

  @Override
  public Optional<InstanceState> handleActorCreationException(final Throwable signalThrowable,
                                                              final ActorInstance actorInstance,
                                                              final ActorInstanceContext context) {
    return handleExceptionInternal(actorInstance, InstanceState.CREATING);
  }

  private Optional<InstanceState> handleExceptionInternal(final ActorInstance actorInstance,
                                                          final InstanceState wakeupState) {
    final Optional<InstanceState> instanceState = incrementAndCheckRetryCounter(Optional.of(maxRestarts)
        .filter(value -> value > 0));

    if (!instanceState.isPresent()) {
      CompletableFuture.delayedExecutor(calculateRestartPeriod().toMillis(),
          TimeUnit.MILLISECONDS,
          timerExecutorService).execute(() -> {
        try {
          actorInstance.transitionState(wakeupState);
        } catch (final IllegalStateException ise) {
          log.warn("Cannot execute state transition on actor {}", actorInstance.getName(), ise);
        }
      });

    }

    return instanceState;
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
    final InstanceState instanceState;

    switch(signal) {
      case PRE_START:
        instanceState = InstanceState.STARTING;
        break;
      case PRE_RESTART:
        instanceState = InstanceState.RESTARTING;
        break;
      default:
        instanceState = InstanceState.STOPPED;
    }

    return instanceState;
  }
}
