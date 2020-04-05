package io.openactors4j.core.impl.common;

import io.openactors4j.core.monitoring.ActorStateEvent;
import io.openactors4j.core.monitoring.ActorStateEventSubscriber;
import io.openactors4j.core.monitoring.ActorStateEventType;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ActorInstanceStateMachineTest {
  private ExecutorService executorService;
  private ActorInstanceStateMachine stateMachine;

  @BeforeEach
  public void createStateMachine() {
    executorService = Executors.newCachedThreadPool();
    stateMachine = new ActorInstanceStateMachine(executorService, "test");

    stateMachine
        .addState(InstanceState.NEW, InstanceState.CREATING, this::createNewInstance)
        .addState(InstanceState.CREATING, InstanceState.STARTING, this::createNewInstance)
        .start(InstanceState.NEW);
  }

  @AfterEach
  public void destroyStateMachine() {
    stateMachine.stop();
    executorService.shutdownNow();
  }

  @Test
  public void shouldExecuteStateTransitionsWithoutMonitoringSubscriber() {
    stateMachine.postStateTransition(InstanceState.CREATING);

    Awaitility.await().atMost(2, TimeUnit.SECONDS)
        .until(() -> stateMachine.getCurrentState() == InstanceState.STARTING);
  }

  @Test
  public void shouldExecuteStateTransitionsWithMonitoringSubscriber() {
    final TestMonitoringSubscriber subscriber = new TestMonitoringSubscriber();

    stateMachine.registerMonitoringSubscriber(subscriber);
    stateMachine.postStateTransition(InstanceState.CREATING);

    Awaitility.await().atMost(2, TimeUnit.SECONDS)
        .until(() -> stateMachine.getCurrentState() == InstanceState.STARTING);

    Assertions.assertThat(subscriber.getEvents())
        .containsExactly(ActorStateEvent.builder()
                .actorName("test")
                .event(ActorStateEventType.ACTOR_CREATING)
                .build(),
            ActorStateEvent.builder()
                .actorName("test")
                .event(ActorStateEventType.ACTOR_STARTING)
                .build());
  }

  private void createNewInstance(final InstanceState sourceState,
                                 final InstanceState targetState,
                                 final ReactiveStateMachine.ReactiveStateUpdater<InstanceState, ActorInstanceStateMachine.ActorInstanceTransitionContext> updater,
                                 final Optional<ActorInstanceStateMachine.ActorInstanceTransitionContext> transitionContext) {
    CompletableFuture.runAsync(() -> updater.postStateTransition(InstanceState.STARTING, null),
        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS, executorService));
  }

  private void startInstance(final InstanceState sourceState,
                             final InstanceState targetState,
                             final ReactiveStateMachine.ReactiveStateUpdater<InstanceState, ActorInstanceStateMachine.ActorInstanceTransitionContext> updater,
                             final Optional<ActorInstanceStateMachine.ActorInstanceTransitionContext> transitionContext) {
  }

  private static class TestMonitoringSubscriber implements ActorStateEventSubscriber {
    @Getter
    private final List<ActorStateEvent> events = new LinkedList<>();

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
      this.subscription = subscription;

      subscription.request(1);
    }

    @Override
    public void onNext(ActorStateEvent item) {
      events.add(item);

      subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }
  }
}
