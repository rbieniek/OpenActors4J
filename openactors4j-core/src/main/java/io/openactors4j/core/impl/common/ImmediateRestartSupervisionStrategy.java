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
  public Optional<InstanceState> handleMessageProcessingException(final Exception processingException,
                                                        final ActorInstance actorInstance,
                                                        final ActorInstanceContext context) {
    return Optional.of(incrementAndCheckRetryCounter(Optional.of(maxRetries)
        .filter(value -> value > 0), InstanceState.STOPPING)
        .orElse(InstanceState.RESTARTING));
  }

  @Override
  public Optional<InstanceState> handleSignalProcessingException(final Throwable signalThrowable,
                                                       final Signal signal,
                                                       final ActorInstance actorInstance,
                                                       final ActorInstanceContext context) {
    return Optional.of(incrementAndCheckRetryCounter(Optional.of(maxRetries)
        .filter(value -> value > 0), InstanceState.STOPPING)
        .orElse(determineStateFromSignal(signal)));
  }

  @Override
  public Optional<InstanceState> handleActorCreationException(final Throwable signalThrowable,
                                                              final ActorInstance actorInstance,
                                                              final ActorInstanceContext context) {
    return Optional.of(incrementAndCheckRetryCounter(Optional.of(maxRetries)
        .filter(value -> value > 0), InstanceState.STOPPED)
        .orElse(InstanceState.CREATING));
  }

  private Optional<InstanceState> incrementAndCheckRetryCounter(final Optional<Integer> maxRetries,
                                                                final InstanceState stopState) {
    return maxRetries
        .filter(value -> value < retryCounter.incrementAndGet())
        .map(value -> stopState);
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
