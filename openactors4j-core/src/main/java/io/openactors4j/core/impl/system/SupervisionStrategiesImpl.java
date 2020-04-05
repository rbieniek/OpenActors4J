package io.openactors4j.core.impl.system;

import io.openactors4j.core.common.SupervisionStrategies;
import io.openactors4j.core.common.SupervisionStrategy;
import io.openactors4j.core.impl.common.BubbleUpSupervisionStrategy;
import io.openactors4j.core.impl.common.DelayedRestartSupervisionStrategy;
import io.openactors4j.core.impl.common.ImmediateRestartSupervisionStrategy;
import io.openactors4j.core.impl.common.TerminateSupervisionStrategy;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SupervisionStrategiesImpl implements SupervisionStrategies {
  private final SupervisionStrategyInternal terminateStrategy = new TerminateSupervisionStrategy();
  private final SupervisionStrategyInternal bubbleUpStrategy = new BubbleUpSupervisionStrategy();

  private final ExecutorService timerExecutorService;

  @Override
  public SupervisionStrategy restart() {
    return new ImmediateRestartSupervisionStrategy(0);
  }

  @Override
  public SupervisionStrategy restart(final int maxRestarts) {
    return new ImmediateRestartSupervisionStrategy(maxRestarts);
  }

  @Override
  public SupervisionStrategy terminate() {
    return terminateStrategy;
  }

  @Override
  public SupervisionStrategy bubbleUp() {
    return bubbleUpStrategy;
  }

  @Override
  public SupervisionStrategy delayedRestart(final Duration restartPeriod) {
    return new DelayedRestartSupervisionStrategy(0,
        restartPeriod,
        Optional.empty(),
        Optional.empty(),
        timerExecutorService);
  }

  @Override
  public SupervisionStrategy delayedRestart(final int maxRestarts,
                                            final Duration restartPeriod) {
    return new DelayedRestartSupervisionStrategy(maxRestarts,
        restartPeriod,
        Optional.empty(),
        Optional.empty(),
        timerExecutorService);
  }

  @Override
  public SupervisionStrategy delayedRestart(final Duration restartPeriod,
                                            final Duration backoffPeriod) {
    return new DelayedRestartSupervisionStrategy(0,
        restartPeriod,
        Optional.ofNullable(backoffPeriod),
        Optional.empty(),
        timerExecutorService);
  }

  @Override
  public SupervisionStrategy delayedRestart(final int maxRestarts,
                                            final Duration restartPeriod,
                                            final Duration backoffPeriod) {
    return new DelayedRestartSupervisionStrategy(maxRestarts,
        restartPeriod,
        Optional.ofNullable(backoffPeriod),
        Optional.empty(),
        timerExecutorService);
  }

  @Override
  public SupervisionStrategy delayedRestart(final Duration restartPeriod,
                                            final Duration backoffPeriod,
                                            final int backoffFactor) {
    return new DelayedRestartSupervisionStrategy(0,
        restartPeriod,
        Optional.ofNullable(backoffPeriod),
        Optional.of(backoffFactor),
        timerExecutorService);
  }

  @Override
  public SupervisionStrategy delayedRestart(final int maxRestarts,
                                            final Duration restartPeriod,
                                            final Duration backoffPeriod,
                                            final int backoffFactor) {
    return new DelayedRestartSupervisionStrategy(maxRestarts,
        restartPeriod,
        Optional.ofNullable(backoffPeriod),
        Optional.of(backoffFactor),
        timerExecutorService);
  }
}
