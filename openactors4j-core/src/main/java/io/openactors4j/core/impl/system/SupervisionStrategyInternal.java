package io.openactors4j.core.impl.system;

import io.openactors4j.core.common.SupervisionStrategy;
import io.openactors4j.core.impl.common.ActorInstance;
import io.openactors4j.core.impl.common.ActorInstanceContext;
import io.openactors4j.core.impl.common.InstanceState;

/**
 * Internal extension interface for modeling common functionality of supervision strategies
 * which are kept internal to the implementation layer
 */
public interface SupervisionStrategyInternal extends SupervisionStrategy {
  InstanceState handleProcessingException(Exception processingException, ActorInstance actorInstance, ActorInstanceContext context);

}
