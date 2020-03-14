package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ImmediateRestartSupervisionStrategy implements SupervisionStrategyInternal {
  private final int maxRetries;

  private AtomicInteger retryCounter = new AtomicInteger(0);

  @Override
  public InstanceState handleMessageProcessingException(final Exception processingException,
                                                        final ActorInstance actorInstance,
                                                        final ActorInstanceContext context) {
    return incrementAndCheckRetryCounter(Optional.of(maxRetries)
        .filter(value -> value > 0))
        .orElse(InstanceState.RUNNING);
  }

  @Override
  public InstanceState handleSignalProcessingException(final Throwable signalThrowable,
                                                       final Signal signal,
                                                       final ActorInstance actorInstance,
                                                       final ActorInstanceContext context) {
    return incrementAndCheckRetryCounter(Optional.of(maxRetries)
        .filter(value -> value > 0))
        .orElse(InstanceState.RUNNING);
  }

  private Optional<InstanceState> incrementAndCheckRetryCounter(final Optional<Integer> maxRetries) {
    return maxRetries
        .filter(value -> value < retryCounter.incrementAndGet())
        .map(value -> InstanceState.STOPPED);
  }
}
