package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;

public class TerminateSupervisionStrategy implements SupervisionStrategyInternal {
  @Override
  public void handleMessageProcessingException(final Throwable processingException,
                                               final ActorInstanceStateTransition transition,
                                               final ActorInstanceContext context) {
    transition.equals(InstanceState.STOPPING);
  }

  @Override
  public void handleActorCreationException(final Throwable signalThrowable,
                                           final ActorInstanceStateTransition transition,
                                           final ActorInstanceContext context) {
    transition.equals(InstanceState.STOPPING);
  }

  @Override
  public void handleSignalProcessingException(final Throwable signalThrowable,
                                              final Signal signal,
                                              final ActorInstanceStateTransition transition,
                                              final ActorInstanceContext context) {
    transition.equals(InstanceState.STOPPING);
  }

}
