package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActorInstanceStateMachine extends ReactiveStateMachine<InstanceState, ActorInstanceStateMachine.ActorInstanceTransitionContext> {
  private static final Map<InstanceState, Boolean> receptionEnabled = new ConcurrentHashMap<>();

  @Data
  @Builder
  public static class ActorInstanceTransitionContext implements ReactiveStateMachine.ReactiveTransitionContext {
    private Throwable throwable;
    private Signal signal;
  }

  static {
    receptionEnabled.put(InstanceState.NEW, false);
    receptionEnabled.put(InstanceState.CREATING, true);
    receptionEnabled.put(InstanceState.CREATE_DELAYED, true);
    receptionEnabled.put(InstanceState.CREATE_FAILED, true);
    receptionEnabled.put(InstanceState.STARTING, true);
    receptionEnabled.put(InstanceState.START_FAILED, true);
    receptionEnabled.put(InstanceState.RUNNING, true);
    receptionEnabled.put(InstanceState.PROCESSING_FAILED, true);
    receptionEnabled.put(InstanceState.RESTARTING, true);
    receptionEnabled.put(InstanceState.RESTARTING_FAILED, true);
    receptionEnabled.put(InstanceState.CREATING, true);
    receptionEnabled.put(InstanceState.CREATING, true);
    receptionEnabled.put(InstanceState.STOPPING, false);
    receptionEnabled.put(InstanceState.STOPPED, false);
  }

  public ActorInstanceStateMachine(ExecutorService executorService, String name) {
    super(executorService, name);
  }

  public void parentalLifecycleEvent(final ParentLifecycleEvent lifecycleEvent) {

  }

  public boolean messageReceptionEnabled() {
    return receptionEnabled.get(getCurrentState());
  }
}
