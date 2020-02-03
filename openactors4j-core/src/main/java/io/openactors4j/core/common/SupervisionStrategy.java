package io.openactors4j.core.common;

import io.openactors4j.core.impl.common.ActorInstance;
import io.openactors4j.core.impl.common.ActorInstanceContext;

public interface SupervisionStrategy {
  void handleProcessingException(Exception processingException, ActorInstance actorInstance, ActorInstanceContext context);
}
