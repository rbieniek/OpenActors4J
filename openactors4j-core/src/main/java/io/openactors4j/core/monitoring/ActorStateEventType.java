package io.openactors4j.core.monitoring;

public enum ActorStateEventType {
  ACTOR_CREATING,
  ACTOR_CREATE_FAILED,
  ACTOR_STARTING,
  ACTOR_START_FAILED,
  ACTOR_RUNNING,
  ACTOR_PROCESSING_FAILED,
  ACTOR_RESTARTING,
  ACTOR_RESTARTING_FAILED,
  ACTOR_STOPPED;
}
