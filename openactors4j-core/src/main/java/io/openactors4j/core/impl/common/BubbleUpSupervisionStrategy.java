package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import java.util.Optional;

public class BubbleUpSupervisionStrategy implements SupervisionStrategyInternal {
  @Override
  public Optional<InstanceState> handleMessageProcessingException(final Exception processingException,
                                                                 final ActorInstance actorInstance,
                                                                 final ActorInstanceContext context) {

    return Optional.empty();
  }

  @Override
  public Optional<InstanceState> handleSignalProcessingException(final Throwable signalThrowable,
                                                       final Signal signal,
                                                       final ActorInstance actorInstance,
                                                       final ActorInstanceContext context) {
    return Optional.empty();
  }

  @Override
  public Optional<InstanceState> handleActorCreationException(final Throwable signalThrowable,
                                                              final ActorInstance actorInstance,
                                                              final ActorInstanceContext context) {
    return Optional.empty();
  }
}
