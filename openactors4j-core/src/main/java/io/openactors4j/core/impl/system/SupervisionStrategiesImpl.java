package io.openactors4j.core.impl.system;

import io.openactors4j.core.common.SupervisionStrategies;
import io.openactors4j.core.common.SupervisionStrategy;
import java.time.Duration;

public class SupervisionStrategiesImpl implements SupervisionStrategies {
  @Override
  public SupervisionStrategy restart() {
    return null;
  }

  @Override
  public SupervisionStrategy terminate() {
    return null;
  }

  @Override
  public SupervisionStrategy bubbleUp() {
    return null;
  }

  @Override
  public SupervisionStrategy delayedRestart(Duration restartPeriod) {
    return null;
  }

  @Override
  public SupervisionStrategy delayedRestart(Duration restartPeriod, Duration backoffPeriod) {
    return null;
  }

  @Override
  public SupervisionStrategy delayedRestart(Duration restartPeriod, Duration backoffPeriod, int backoffFactor) {
    return null;
  }
}
