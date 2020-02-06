package io.openactors4j.core.impl.system;

import static java.util.Optional.of;


import io.openactors4j.core.common.SupervisionStrategies;
import io.openactors4j.core.common.SupervisionStrategy;
import io.openactors4j.core.impl.common.BubbleUpSupervisionStrategy;
import io.openactors4j.core.impl.common.DelayedRestartSupervisionStrategy;
import io.openactors4j.core.impl.common.ImmediateRestartSupervisionStrategy;
import io.openactors4j.core.impl.common.TerminateSupervisionStrategy;
import java.time.Duration;

public class SupervisionStrategiesImpl implements SupervisionStrategies {
  private final SupervisionStrategyInternal immediateRestartStrategy = new ImmediateRestartSupervisionStrategy();
  private final SupervisionStrategyInternal terminaStrategy = new TerminateSupervisionStrategy();
  private final SupervisionStrategyInternal bubbleUpStrategy = new BubbleUpSupervisionStrategy();

  @Override
  public SupervisionStrategy restart() {
    return immediateRestartStrategy;
  }

  @Override
  public SupervisionStrategy terminate() {
    return terminaStrategy;
  }

  @Override
  public SupervisionStrategy bubbleUp() {
    return bubbleUpStrategy;
  }

  @Override
  public SupervisionStrategy delayedRestart(final Duration restartPeriod) {
    return DelayedRestartSupervisionStrategy.builder()
        .restartPeriod(restartPeriod)
        .build();
  }

  @Override
  public SupervisionStrategy delayedRestart(final Duration restartPeriod, final Duration backoffPeriod) {
    return DelayedRestartSupervisionStrategy.builder()
        .restartPeriod(restartPeriod)
        .backoffPeriod(of(backoffPeriod))
        .build();
  }

  @Override
  public SupervisionStrategy delayedRestart(final Duration restartPeriod, final Duration backoffPeriod, final int backoffFactor) {
    return DelayedRestartSupervisionStrategy.builder()
        .restartPeriod(restartPeriod)
        .backoffPeriod(of(backoffPeriod))
        .backoffFactor(of(backoffFactor))
        .build();
  }
}
