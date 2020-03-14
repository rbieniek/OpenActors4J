package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;

public class BubbleUpSupervisionStrategy implements SupervisionStrategyInternal {
  @Override
  public InstanceState handleMessageProcessingException(final Exception processingException,
                                                        final ActorInstance actorInstance,
                                                        final ActorInstanceContext context) {

    return InstanceState.SUSPENDED;
  }

  @Override
  public InstanceState handleSignalProcessingException(final Throwable signalThrowable,
                                                       final Signal signal,
                                                       final ActorInstance actorInstance,
                                                       final ActorInstanceContext context) {
    return InstanceState.SUSPENDED;
  }
}
