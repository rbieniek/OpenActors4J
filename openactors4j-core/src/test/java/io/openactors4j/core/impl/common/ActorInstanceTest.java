package io.openactors4j.core.impl.common;

import static org.assertj.core.api.Assertions.assertThat;


import io.openactors4j.core.common.Actor;
import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.ActorRef;
import io.openactors4j.core.common.DeathNote;
import io.openactors4j.core.common.NotImplementedException;
import io.openactors4j.core.common.Signal;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.common.SupervisionStrategies;
import io.openactors4j.core.common.SystemAddress;
import io.openactors4j.core.common.UnboundedMailbox;
import io.openactors4j.core.impl.messaging.ExtendedMessage;
import io.openactors4j.core.impl.messaging.Message;
import io.openactors4j.core.impl.messaging.RoutingSlip;
import io.openactors4j.core.impl.messaging.SystemAddressImpl;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import io.openactors4j.core.monitoring.ActorActionEvent;
import io.openactors4j.core.monitoring.ActorActionEventSubscriber;
import io.openactors4j.core.monitoring.ActorActionEventType;
import io.openactors4j.core.monitoring.ActorOutcomeType;
import io.openactors4j.core.monitoring.ActorSignalEvent;
import io.openactors4j.core.monitoring.ActorSignalEventSubscriber;
import io.openactors4j.core.monitoring.ActorSignalType;
import io.openactors4j.core.typed.BehaviorBuilder;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ActorInstanceTest {
  private ReactiveMailboxScheduler<Integer> scheduler;

  private SystemAddress testAddress;
  private SystemAddress sourceAddress;

  @BeforeEach
  public void setupAdresses() {
    testAddress = SystemAddressImpl.builder()
        .hostname("localhost")
        .systemName("test-system")
        .transportScheme("local")
        .path("/test")
        .build();
    sourceAddress = SystemAddressImpl.builder()
        .hostname("localhost")
        .systemName("test-system")
        .transportScheme("local")
        .path("/source")
        .build();
  }

  @BeforeEach
  public void setupScheduler() {
    scheduler = new ReactiveMailboxScheduler<>(Executors.newCachedThreadPool());

    scheduler.initialize();
  }

  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessage() {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final TestActorInstance<WorkingTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(recorder.getActionEvents()).isEmpty();
    assertThat(recorder.getSignalEvents()).isEmpty();

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> !recorder.getActionEvents().isEmpty() && !recorder.getSignalEvents().isEmpty());
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS));

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getPayloads().size() > 0);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> recorder.getActionEvents().size() > 1);
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorActionEventType.MESSAGE_DELIVERY, ActorOutcomeType.SUCCESS));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS));
  }

  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessageAndTerminate() {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final TestActorInstance<WorkingTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);
    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    assertThat(actorInstance.getPayloads()).isEmpty();

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> !recorder.getActionEvents().isEmpty() && !recorder.getSignalEvents().isEmpty());
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS));

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getPayloads().size() > 0);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> recorder.getActionEvents().size() > 1);
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorActionEventType.MESSAGE_DELIVERY, ActorOutcomeType.SUCCESS));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS));

    actorInstance.routeMessage(new ExtendedMessage<Integer, DeathNote>(targetSlip, sourceAddress, DeathNote.INSTANCE));

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.STOPPED);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START, Signal.POST_STOP);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> recorder.getSignalEventTypes().size() > 1);
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorSignalType.SIGNAL_POST_STOP, ActorOutcomeType.SUCCESS));
  }

  @Test
  public void shouldStopOnCreationFailingActorWithImmediateSupervisionStrategy() {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final WorkingTestActorInstance<FailedCreationTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(
        FailedCreationTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.STOPPED);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getReceivedSignals()).isEmpty();
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> !recorder.getActionEvents().isEmpty());
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.FAILURE));
  }

  @Test
  public void shouldStopOnSlowCreationActorWithImmediateSupervisionStrategy() {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final WorkingTestActorInstance<SlowCreationTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(
        SlowCreationTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.STOPPED);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getReceivedSignals()).isEmpty();
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> !recorder.getActionEvents().isEmpty());
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.FAILURE));
  }

  @Test
  public void shouldStopOnCreationFailingActorWithDelayedSupervisionStrategy() {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final WorkingTestActorInstance<FailedCreationTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(
        FailedCreationTestActor::new,
        "test",
        new DelayedRestartSupervisionStrategy(1,
            Duration.of(5, ChronoUnit.SECONDS),
            Optional.empty(),
            Optional.empty(),
            Executors.newSingleThreadExecutor()),
        StartupMode.IMMEDIATE);
    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    Awaitility.await()
        .atMost(2, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.CREATE_FAILED);
    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.STOPPED);

    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getReceivedSignals()).isEmpty();
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> !recorder.getActionEvents().isEmpty());
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.FAILURE));
  }

  @Test
  public void shouldCreateAndRecreateStartedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessage() {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final TestActorInstance<Actor, Integer> actorInstance = new WorkingTestActorInstance(SwitchingActorSupplier.builder()
        .supplier(FailedCreationTestActor::new)
        .supplier(WorkingTestActor::new)
        .build(),
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);
    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    assertThat(actorInstance.getPayloads()).isEmpty();

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> !recorder.getActionEvents().isEmpty() && !recorder.getSignalEvents().isEmpty());
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS));

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getPayloads().size() > 0);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);
  }

  @Test
  public void shouldCreateImmediateFailingSignalActorWithImmediateStartAndImmediateSupervision() {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final SignalHandlingFailureTestActorInstance<WorkingTestActor, Integer> actorInstance = new SignalHandlingFailureTestActorInstance<>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE,
        Signal.PRE_START);
    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.STOPPED);

    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getReceivedSignals())
        .containsExactly(Signal.PRE_START, Signal.PRE_START, Signal.POST_STOP);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> !recorder.getActionEvents().isEmpty() && !recorder.getSignalEvents().isEmpty());
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorSignalType.SIGNAL_POST_STOP, ActorOutcomeType.SUCCESS));
  }

  @Test
  public void shouldCreateImmediateSlowSignalActorWithImmediateStartAndImmediateSupervision() {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final SlowSignalHandlingTestActorInstance<WorkingTestActor, Integer> actorInstance = new SlowSignalHandlingTestActorInstance<>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE,
        Signal.PRE_START);
    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.STOPPED);

    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getReceivedSignals())
        .containsExactly(Signal.PRE_START, Signal.PRE_START, Signal.POST_STOP);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> !recorder.getActionEvents().isEmpty() && !recorder.getSignalEvents().isEmpty());
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorSignalType.SIGNAL_POST_STOP, ActorOutcomeType.SUCCESS));
  }

  @Test
  public void shouldCreateRestartingDelayedFailingSignalActorWithImmediateStartAndImmediateSupervisionAndMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final SignalHandlingFailureTestActorInstance<WorkingTestActor, Integer> actorInstance = new SignalHandlingFailureTestActorInstance<>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE,
        Signal.PRE_START);
    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.STOPPED);

    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getReceivedSignals())
        .containsExactly(Signal.PRE_START, Signal.PRE_START, Signal.POST_STOP);
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);
    assertThat(actorInstance.getPayloads()).isEmpty();

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> !recorder.getActionEvents().isEmpty() && !recorder.getSignalEvents().isEmpty());
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorSignalType.SIGNAL_POST_STOP, ActorOutcomeType.SUCCESS));
    assertThat(actorInstance.getPayloads()).isEmpty();
  }

  @Test
  public void shouldCreateDelayedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessage() {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final WorkingTestActorInstance<WorkingTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.DELAYED);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);
    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    assertThat(actorInstance.getPayloads()).isEmpty();

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.CREATE_DELAYED);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.CREATE_DELAYED);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).isEmpty();

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> recorder.getActionEvents().size() > 1);
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorActionEventType.MESSAGE_DELIVERY, ActorOutcomeType.SUCCESS));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS));
  }

  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndFailedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final TestActorInstance<WorkingTestActor, Integer> actorInstance = new MessageHandlingFailureTestActorInstance<>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);

    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> actorInstance.getReceivedSignals().contains(Signal.PRE_RESTART));
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START, Signal.PRE_RESTART);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> recorder.getActionEvents().size() > 1);
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorActionEventType.MESSAGE_DELIVERY, ActorOutcomeType.FAILURE));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorSignalType.SIGNAL_PRE_RESTART, ActorOutcomeType.SUCCESS));
  }

  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndTwoFailedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final TestActorInstance<WorkingTestActor, Integer> actorInstance = new MessageHandlingFailureTestActorInstance<>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);

    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> actorInstance.getReceivedSignals().contains(Signal.PRE_RESTART));
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START,
        Signal.PRE_RESTART);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.STOPPED);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getPayloads()).containsExactly(1, 1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START,
        Signal.PRE_RESTART,
        Signal.POST_STOP);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> recorder.getActionEvents().size() > 1);
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorActionEventType.MESSAGE_DELIVERY, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorActionEventType.MESSAGE_DELIVERY, ActorOutcomeType.FAILURE));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorSignalType.SIGNAL_PRE_RESTART, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorSignalType.SIGNAL_POST_STOP, ActorOutcomeType.SUCCESS));
  }

  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndFailedMessageAndRestartFailure() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final TestActorInstance<WorkingTestActor, Integer> actorInstance = new MessageHandlingAndSlowSignalTestActorInstance<WorkingTestActor, Integer>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE,
        Signal.PRE_RESTART);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);

    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.STOPPED);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START,
        Signal.PRE_RESTART,
        Signal.POST_STOP);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> recorder.getActionEvents().size() > 1);
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorActionEventType.MESSAGE_DELIVERY, ActorOutcomeType.FAILURE));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorSignalType.SIGNAL_PRE_RESTART, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorSignalType.SIGNAL_POST_STOP, ActorOutcomeType.SUCCESS));
  }

  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndFailedMessageAndSlowRestart() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final TestActorInstance<WorkingTestActor, Integer> actorInstance = new MessageHandlingAndSignalFailureTestActorInstance<WorkingTestActor, Integer>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE,
        Signal.PRE_RESTART);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);

    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.STOPPED);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START,
        Signal.PRE_RESTART,
        Signal.POST_STOP);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> recorder.getActionEvents().size() > 1);
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorActionEventType.MESSAGE_DELIVERY, ActorOutcomeType.FAILURE));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorSignalType.SIGNAL_PRE_RESTART, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorSignalType.SIGNAL_POST_STOP, ActorOutcomeType.SUCCESS));
  }

  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndFailedMessageAndOneRestartFailure() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final TestActorInstance<WorkingTestActor, Integer> actorInstance = new MessageHandlingAndSignalFailureTestActorInstance<WorkingTestActor, Integer>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(2),
        StartupMode.IMMEDIATE,
        Signal.PRE_RESTART);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    final TestMonitoringEventRecorder recorder = new TestMonitoringEventRecorder();

    actorInstance.initializeAndStart(actorInstanceContext);
    recorder.subscribeMonitoringEvents(actorInstance);

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);

    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> recorder.getActionEvents().size() > 1);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(recorder.getActionEventTypes())
        .containsExactly(ImmutablePair.of(ActorActionEventType.CREATE_INSTANCE, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorActionEventType.MESSAGE_DELIVERY, ActorOutcomeType.FAILURE));
    assertThat(recorder.getSignalEventTypes())
        .containsExactly(ImmutablePair.of(ActorSignalType.SIGNAL_PRE_START, ActorOutcomeType.SUCCESS),
            ImmutablePair.of(ActorSignalType.SIGNAL_PRE_RESTART, ActorOutcomeType.FAILURE),
            ImmutablePair.of(ActorSignalType.SIGNAL_PRE_RESTART, ActorOutcomeType.SUCCESS));
  }

  @Slf4j
  @Getter
  private static final class TestMonitoringEventRecorder {
    private final List<ActorActionEvent> actionEvents = new LinkedList<>();
    private final List<ActorSignalEvent> signalEvents = new LinkedList<>();

    public List<Pair<ActorActionEventType, ActorOutcomeType>> getActionEventTypes() {
      return actionEvents.stream()
          .map(event -> ImmutablePair.of(event.getAction(), event.getOutcome()))
          .collect(Collectors.toList());
    }

    public List<Pair<ActorSignalType, ActorOutcomeType>> getSignalEventTypes() {
      return signalEvents.stream()
          .map(event -> ImmutablePair.of(event.getSignal(), event.getOutcome()))
          .collect(Collectors.toList());
    }

    public void subscribeMonitoringEvents(final ActorInstance instance) {
      instance.registerActionEventSubscriber(new ActorActionEventSubscriber() {
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
          this.subscription = subscription;

          subscription.request(1);
        }

        @Override
        public void onNext(ActorActionEvent item) {
          log.info("Received action event {}", item);

          actionEvents.add(item);

          subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
          log.error("Exception caught in monitoring action event subscriber", throwable);
        }

        @Override
        public void onComplete() {
          log.error("Closing the monitoring action event subscriber");
        }
      });

      instance.registerSignalEventSubscriber(new ActorSignalEventSubscriber() {
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
          this.subscription = subscription;

          subscription.request(1);
        }

        @Override
        public void onNext(ActorSignalEvent item) {
          log.info("Received signal event {}", item);

          signalEvents.add(item);

          subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
          log.error("Exception caught in monitoring signal event subscriber", throwable);
        }

        @Override
        public void onComplete() {
          log.error("Closing the monitoring signal event subscriber");
        }
      });
    }
  }

  @RequiredArgsConstructor
  private static final class TestActorInstanceContext<T> implements ActorInstanceContext<T> {
    private final MailboxScheduler<T> mailboxScheduler;

    public enum TestInstanceState {
      ACTIVE,
      STOPPED;
    }

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Getter
    private final List<Message<T>> undeliverableMessages = new LinkedList<>();

    @Getter
    private TestInstanceState instanceState = TestInstanceState.ACTIVE;

    private ActorInstance<? extends Actor, T> actorInstance;

    private FlowControlledMailbox<Message<T>> mailbox;

    @Override
    public ExecutorService provideMonitoringExecutor() {
      return executorService;
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
      return CompletableFuture.runAsync(runnable, executorService);
    }

    @Override
    public ActorInstanceStateMachine provideStateMachine(final String name) {
      return new ActorInstanceStateMachine(executorService, name);
    }

    @Override
    public void enqueueMessage(Message<T> message) {
      mailbox.putMessage(message);
    }

    @Override
    public void undeliverableMessage(Message<T> message) {
      undeliverableMessages.add(message);
    }

    @Override
    public <V extends Actor> void assignAndCreate(ActorInstance<V, T> actorInstance) {
      this.actorInstance = actorInstance;
      mailbox = mailboxScheduler.registerMailbox(new UnboundedMailbox<>(),
          () -> mailbox.takeMessage().ifPresent(message -> actorInstance.processMessage(message)));

      mailbox.startReceiving();
      actorInstance.triggerActorCreation();
    }

    @Override
    public void terminateProcessing() {
      mailbox.stopReceiving();
      this.instanceState = TestInstanceState.STOPPED;
    }

    @Override
    public ActorInstance parentActor() {
      throw new NotImplementedException();
    }

    @Override
    public <V> BehaviorBuilder<V> newBehaviorBuilder() {
      return null;
    }

    @Override
    public UntypedActorBuilder newUntypedActorBuilder() {
      return null;
    }

    @Override
    public SupervisionStrategies supervisionStrategies() {
      return null;
    }

    @Override
    public ActorRef actorRefForAddress(SystemAddress address) {
      return null;
    }

    @Override
    public Duration actorTimeout() {
      return Duration.of(1, ChronoUnit.SECONDS);
    }

    @Override
    public void enableMessageDelivery() {
      mailbox.processMode(FlowControlledMailbox.ProcessMode.DELIVER);
    }

    @Override
    public void disableMessageDelivery() {
      mailbox.processMode(FlowControlledMailbox.ProcessMode.QUEUE);
    }

  }

  private static class TestActorInstance<V extends Actor, T> extends ActorInstance<V, T> {
    @Getter
    private List<Signal> receivedSignals = new LinkedList<>();

    @Getter
    private final List<T> payloads = Collections.synchronizedList(new LinkedList<>());

    protected TestActorInstance(Callable<V> supplier, String name, SupervisionStrategyInternal supervisionStrategy, StartupMode startupMode) {
      super(supplier, name, supervisionStrategy, startupMode);
    }

    @Override
    protected final void sendMessageToActor(final Message<T> message) {
      payloads.add(message.getPayload());

      handleMessageInternal(message);
    }

    protected void handleMessageInternal(final Message<T> message) {

    }

    @Override
    protected final void sendSignal(final Signal signal) {

      receivedSignals.add(signal);
      sendSignalInternal(signal);
    }

    protected void sendSignalInternal(final Signal signal) {

    }
  }

  private static class WorkingTestActorInstance<V extends Actor, T> extends TestActorInstance<V, T> {

    public WorkingTestActorInstance(Callable<V> supplier,
                                    String name,
                                    SupervisionStrategyInternal supervisionStrategy,
                                    StartupMode startupMode) {
      super(supplier, name, supervisionStrategy, startupMode);
    }
  }

  private static class MessageHandlingFailureTestActorInstance<V extends Actor, T> extends TestActorInstance<V, T> {
    public MessageHandlingFailureTestActorInstance(final Callable<V> supplier,
                                                   final String name,
                                                   final SupervisionStrategyInternal supervisionStrategy,
                                                   final StartupMode startupMode) {
      super(supplier, name, supervisionStrategy, startupMode);
    }

    @Override
    protected final void handleMessageInternal(Message<T> message) {
      throw new IllegalArgumentException();
    }
  }

  private static class SignalHandlingFailureTestActorInstance<V extends Actor, T> extends TestActorInstance<V, T> {
    private final Set<Signal> failingSignals;

    public SignalHandlingFailureTestActorInstance(final Callable<V> supplier,
                                                  final String name,
                                                  final SupervisionStrategyInternal supervisionStrategy,
                                                  final StartupMode startupMode,
                                                  final Signal failingSignal) {
      super(supplier, name, supervisionStrategy, startupMode);

      this.failingSignals = Collections.singleton(failingSignal);
    }

    public SignalHandlingFailureTestActorInstance(final Callable<V> supplier,
                                                  final String name,
                                                  final SupervisionStrategyInternal supervisionStrategy,
                                                  final StartupMode startupMode,
                                                  final Set<Signal> failingSignals) {
      super(supplier, name, supervisionStrategy, startupMode);

      this.failingSignals = Collections.unmodifiableSet(failingSignals);
    }

    protected final void sendSignalInternal(final Signal signal) {
      if (failingSignals.contains(signal)) {
        throw new IllegalArgumentException();
      }
    }
  }

  private static class SlowSignalHandlingTestActorInstance<V extends Actor, T> extends TestActorInstance<V, T> {
    private final Set<Signal> slowSignals;

    public SlowSignalHandlingTestActorInstance(final Callable<V> supplier,
                                                  final String name,
                                                  final SupervisionStrategyInternal supervisionStrategy,
                                                  final StartupMode startupMode,
                                                  final Signal failingSignal) {
      super(supplier, name, supervisionStrategy, startupMode);

      this.slowSignals = Collections.singleton(failingSignal);
    }

    public SlowSignalHandlingTestActorInstance(final Callable<V> supplier,
                                                  final String name,
                                                  final SupervisionStrategyInternal supervisionStrategy,
                                                  final StartupMode startupMode,
                                                  final Set<Signal> slowSignals) {
      super(supplier, name, supervisionStrategy, startupMode);

      this.slowSignals = Collections.unmodifiableSet(slowSignals);
    }

    @SneakyThrows
    protected final void sendSignalInternal(final Signal signal)  {
      if (slowSignals.contains(signal)) {
        Thread.sleep(3000);
      }
    }
  }

  private static class MessageHandlingAndSignalFailureTestActorInstance<V extends Actor, T> extends TestActorInstance<V, T> {
    private final List<Signal> failingSignals = new LinkedList<>();

    public MessageHandlingAndSignalFailureTestActorInstance(final Callable<V> supplier,
                                                            final String name,
                                                            final SupervisionStrategyInternal supervisionStrategy,
                                                            final StartupMode startupMode,
                                                            final Signal failingSignal) {
      super(supplier, name, supervisionStrategy, startupMode);

      this.failingSignals.add(failingSignal);
    }

    public MessageHandlingAndSignalFailureTestActorInstance(final Callable<V> supplier,
                                                            final String name,
                                                            final SupervisionStrategyInternal supervisionStrategy,
                                                            final StartupMode startupMode,
                                                            final List<Signal> failingSignals) {
      super(supplier, name, supervisionStrategy, startupMode);

      this.failingSignals.addAll(failingSignals);
    }

    protected final void sendSignalInternal(final Signal signal) {
      if (failingSignals.contains(signal)) {
        failingSignals.remove(0);

        throw new IllegalArgumentException();
      }
    }

    @Override
    protected final void handleMessageInternal(Message<T> message) {
      throw new IllegalArgumentException();
    }
  }


  private static class MessageHandlingAndSlowSignalTestActorInstance<V extends Actor, T> extends TestActorInstance<V, T> {
    private final List<Signal> slowSignals = new LinkedList<>();

    public MessageHandlingAndSlowSignalTestActorInstance(final Callable<V> supplier,
                                                            final String name,
                                                            final SupervisionStrategyInternal supervisionStrategy,
                                                            final StartupMode startupMode,
                                                            final Signal failingSignal) {
      super(supplier, name, supervisionStrategy, startupMode);

      this.slowSignals.add(failingSignal);
    }

    public MessageHandlingAndSlowSignalTestActorInstance(final Callable<V> supplier,
                                                            final String name,
                                                            final SupervisionStrategyInternal supervisionStrategy,
                                                            final StartupMode startupMode,
                                                            final List<Signal> slowSignals) {
      super(supplier, name, supervisionStrategy, startupMode);

      this.slowSignals.addAll(slowSignals);
    }

    @SneakyThrows
    protected final void sendSignalInternal(final Signal signal) {
      if (slowSignals.contains(signal)) {
        slowSignals.remove(0);

        Thread.sleep(3000);
      }
    }

    @Override
    protected final void handleMessageInternal(Message<T> message) {
      throw new IllegalArgumentException();
    }
  }

  private static class WorkingTestActor implements Actor {
    @Getter
    private ActorContext context;

    @Override
    public void setupContext(ActorContext context) {
      this.context = context;
    }
  }

  private static class FailedCreationTestActor implements Actor {
    @Getter
    private ActorContext context;

    private FailedCreationTestActor() {
      throw new IllegalArgumentException();
    }

    @Override
    public void setupContext(ActorContext context) {
      this.context = context;
    }
  }

  private static class SlowCreationTestActor implements Actor {
    @Getter
    private ActorContext context;

    private SlowCreationTestActor() throws InterruptedException {
      Thread.sleep(3000);
    }

    @Override
    public void setupContext(ActorContext context) {
      this.context = context;
    }
  }

  private static class SystemTestActor implements Actor, SystemActor {
    @Getter
    private ActorContext context;

    @Override
    public void setupContext(ActorContext context) {
      this.context = context;
    }
  }

  @Data
  @Builder
  private static class SwitchingActorSupplier<T> implements Callable<T> {
    @Singular
    private List<Callable<T>> suppliers;

    private int index;

    @Override
    public T call() throws Exception {
      return suppliers.get(index++).call();
    }
  }
}