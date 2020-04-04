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

  private static final Map<InstanceState, ActorStateEventType> monitoringMap = new ConcurrentHashMap<>();

  static {
    monitoringMap.put(CREATING, ActorStateEventType.ACTOR_CREATING);
    monitoringMap.put(CREATE_FAILED, ActorStateEventType.ACTOR_CREATE_FAILED);
    monitoringMap.put(STARTING, ActorStateEventType.ACTOR_STARTING);
    monitoringMap.put(START_FAILED, ActorStateEventType.ACTOR_START_FAILED);
    monitoringMap.put(RUNNING, ActorStateEventType.ACTOR_RUNNING);
    monitoringMap.put(PROCESSING_FAILED, ActorStateEventType.ACTOR_PROCESSING_FAILED);
    monitoringMap.put(RESTARTING, ActorStateEventType.ACTOR_RESTARTING_FAILED);
    monitoringMap.put(RESTART_FAILED, ActorStateEventType.ACTOR_RESTARTING_FAILED);
    monitoringMap.put(STOPPED, ActorStateEventType.ACTOR_STOPPED);
  }

  public Optional<ActorStateEventType> toMonitoringType() {
    return Optional.ofNullable(monitoringMap.get(this));
  }
}
