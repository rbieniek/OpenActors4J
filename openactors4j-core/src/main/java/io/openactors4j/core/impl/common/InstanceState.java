package io.openactors4j.core.impl.common;

public enum InstanceState {
  NEW,
  STOPPED,
  STARTING,
  RUNNING,
  RESTARTING,
  RESTARTING_DELAYED,
  DELAYED,
  SUSPENDED;
}
