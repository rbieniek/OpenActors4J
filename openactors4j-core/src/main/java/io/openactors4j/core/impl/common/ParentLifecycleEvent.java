package io.openactors4j.core.impl.common;

/**
 * THis class models the lifecycle events which are passed from a parent actor instance to
 * is child instances
 */
public enum ParentLifecycleEvent {
  RESTARTING,
  STOPPED;
}
