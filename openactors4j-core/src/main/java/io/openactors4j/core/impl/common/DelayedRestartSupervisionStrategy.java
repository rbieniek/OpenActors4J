package io.openactors4j.core.impl.common;

import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import java.time.Duration;
import java.util.Optional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Builder
public class DelayedRestartSupervisionStrategy implements SupervisionStrategyInternal {
  private final Duration restartPeriod;

  @Builder.Default
  private final Optional<Duration> backoffPeriod = Optional.empty();

  @Builder.Default
  private final Optional<Integer> backoffFactor = Optional.empty();

  @Override
  public void handleProcessingException(Exception processingException, ActorInstance actorInstance, ActorInstanceContext context) {

  }
}
