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
  public InstanceState handleMessageProcessingException(final Exception processingException,
                                                        final ActorInstance actorInstance,
                                                        final ActorInstanceContext context) {
    return handleExceptionInternal(actorInstance);
  }

  @Override
  public InstanceState handleSignalProcessingException(final Throwable signalThrowable,
                                                       final Signal signal,
                                                       final ActorInstance actorInstance,
                                                       final ActorInstanceContext context) {
    return handleExceptionInternal(actorInstance);
  }

  private InstanceState handleExceptionInternal(final ActorInstance actorInstance) {
    final InstanceState instanceState = incrementAndCheckRetryCounter(Optional.of(maxRestarts)
        .filter(value -> value > 0))
        .orElse(InstanceState.RESTARTING_DELAYED);

    if (instanceState == InstanceState.RESTARTING_DELAYED) {
      CompletableFuture.delayedExecutor(calculateRestartPeriod().toMillis(),
          TimeUnit.MILLISECONDS,
          timerExecutorService).execute(() -> {
        try {
          actorInstance.transitionState(InstanceState.RUNNING);
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
}
