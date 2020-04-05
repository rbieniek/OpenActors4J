package io.openactors4j.core.impl.common;

import io.openactors4j.core.monitoring.ActorStateEventType;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public enum InstanceState {
  NEW,
  CREATING,
  CREATE_DELAYED,
  CREATE_FAILED,
  STARTING,
  START_FAILED,
  RUNNING,
  PROCESSING_FAILED,
  RESTARTING,
  RESTART_FAILED,
  STOPPING,
  STOPPED;

  private static final Map<InstanceState, ActorStateEventType> MONITORING_MAP = new ConcurrentHashMap<>();

  static {
    MONITORING_MAP.put(CREATING, ActorStateEventType.ACTOR_CREATING);
    MONITORING_MAP.put(CREATE_FAILED, ActorStateEventType.ACTOR_CREATE_FAILED);
    MONITORING_MAP.put(STARTING, ActorStateEventType.ACTOR_STARTING);
    MONITORING_MAP.put(START_FAILED, ActorStateEventType.ACTOR_START_FAILED);
    MONITORING_MAP.put(RUNNING, ActorStateEventType.ACTOR_RUNNING);
    MONITORING_MAP.put(PROCESSING_FAILED, ActorStateEventType.ACTOR_PROCESSING_FAILED);
    MONITORING_MAP.put(RESTARTING, ActorStateEventType.ACTOR_RESTARTING_FAILED);
    MONITORING_MAP.put(RESTART_FAILED, ActorStateEventType.ACTOR_RESTARTING_FAILED);
    MONITORING_MAP.put(STOPPED, ActorStateEventType.ACTOR_STOPPED);
  }

  public Optional<ActorStateEventType> toMonitoringType() {
    return Optional.ofNullable(MONITORING_MAP.get(this));
  }
}
