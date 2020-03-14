package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;

public class TerminateSupervisionStrategy implements SupervisionStrategyInternal {
  @Override
  public InstanceState handleMessageProcessingException(final Exception processingException,
                                                        final ActorInstance actorInstance,
                                                        final ActorInstanceContext context) {

    return InstanceState.STOPPED;
  }

  @Override
  public InstanceState handleSignalProcessingException(final Throwable signalThrowable,
                                                       final Signal signal,
                                                       final ActorInstance actorInstance,
                                                       final ActorInstanceContext context) {
    return InstanceState.STOPPED;
  }

}
