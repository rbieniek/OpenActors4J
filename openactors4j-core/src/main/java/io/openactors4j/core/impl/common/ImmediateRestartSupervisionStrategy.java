package io.openactors4j.core.impl.common;

import io.openactors4j.core.impl.system.SupervisionStrategyInternal;

public class ImmediateRestartSupervisionStrategy implements SupervisionStrategyInternal {
  @Override
  public void handleProcessingException(Exception processingException, ActorInstance actorInstance, ActorInstanceContext context) {

  }
}
