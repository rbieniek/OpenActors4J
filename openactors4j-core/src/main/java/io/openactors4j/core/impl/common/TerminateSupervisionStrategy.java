package io.openactors4j.core.impl.common;

import io.openactors4j.core.impl.system.SupervisionStrategyInternal;

public class TerminateSupervisionStrategy implements SupervisionStrategyInternal {
  @Override
  public InstanceState handleProcessingException(final Exception processingException, final ActorInstance actorInstance, final ActorInstanceContext context) {

    return InstanceState.STOPPED;
  }
}
