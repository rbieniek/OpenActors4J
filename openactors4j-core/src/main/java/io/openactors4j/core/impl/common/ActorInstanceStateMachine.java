package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Signal;
import io.openactors4j.core.monitoring.ActorStateEvent;
import io.openactors4j.core.monitoring.ActorStateEventSubscriber;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActorInstanceStateMachine extends ReactiveStateMachine<InstanceState, ActorInstanceStateMachine.ActorInstanceTransitionContext> {
  private static final Map<InstanceState, Boolean> RECEPTION_ENABLED = new ConcurrentHashMap<>();

  private final SubmissionPublisher<ActorStateEvent> monitoringPublisher;

  @Data
  @Builder
  public static class ActorInstanceTransitionContext implements ReactiveStateMachine.ReactiveTransitionContext {
    private Throwable throwable;
    private Signal signal;
  }

  static {
    RECEPTION_ENABLED.put(InstanceState.NEW, false);
    RECEPTION_ENABLED.put(InstanceState.CREATING, true);
    RECEPTION_ENABLED.put(InstanceState.CREATE_DELAYED, true);
    RECEPTION_ENABLED.put(InstanceState.CREATE_FAILED, true);
    RECEPTION_ENABLED.put(InstanceState.STARTING, true);
    RECEPTION_ENABLED.put(InstanceState.START_FAILED, true);
    RECEPTION_ENABLED.put(InstanceState.RUNNING, true);
    RECEPTION_ENABLED.put(InstanceState.PROCESSING_FAILED, true);
    RECEPTION_ENABLED.put(InstanceState.RESTARTING, true);
    RECEPTION_ENABLED.put(InstanceState.RESTART_FAILED, true);
    RECEPTION_ENABLED.put(InstanceState.CREATING, true);
    RECEPTION_ENABLED.put(InstanceState.CREATING, true);
    RECEPTION_ENABLED.put(InstanceState.STOPPING, false);
    RECEPTION_ENABLED.put(InstanceState.STOPPED, false);
  }

  public ActorInstanceStateMachine(final ExecutorService executorService, final String name) {
    super(executorService, name);

    monitoringPublisher = new SubmissionPublisher<>(executorService, Flow.defaultBufferSize());
  }

  public ActorInstanceStateMachine registerMonitoringSubscriber(final ActorStateEventSubscriber subscriber) {
    monitoringPublisher.subscribe(subscriber);

    return this;
  }


  public void parentalLifecycleEvent(final ParentLifecycleEvent lifecycleEvent) {

  }

  public boolean messageReceptionEnabled() {
    return RECEPTION_ENABLED.get(getCurrentState());
  }

  @Override
  protected void emitMonitoringEvent(final InstanceState state) {
    if (monitoringPublisher.hasSubscribers()) {
      state.toMonitoringType()
          .ifPresent(event -> monitoringPublisher.submit(ActorStateEvent.builder()
              .actorName(getName())
              .event(event)
              .build()));
    }
  }

  @Override
  protected void shutdownHook() {
    monitoringPublisher.close();
  }

}
