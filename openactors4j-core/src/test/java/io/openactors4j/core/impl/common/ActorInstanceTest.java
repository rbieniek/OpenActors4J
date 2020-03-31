package io.openactors4j.core.impl.common;

import static org.assertj.core.api.Assertions.assertThat;


import io.openactors4j.core.common.Actor;
import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.ActorRef;
import io.openactors4j.core.common.DeathNote;
import io.openactors4j.core.common.Mailbox;
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
import io.openactors4j.core.typed.BehaviorBuilder;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
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
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final TestActorInstance<WorkingTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstance.initializeAndStart(actorInstanceContext);
    assertThat(actorInstance.getPayloads()).isEmpty();

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

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
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessageAndTerminate() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final TestActorInstance<WorkingTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstance.initializeAndStart(actorInstanceContext);
    assertThat(actorInstance.getPayloads()).isEmpty();

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

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
  }

  @Test
  public void shouldStopOnCreationFailingActorWithImmediateSupervisionStrategy() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final WorkingTestActorInstance<FailedCreationTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(
        FailedCreationTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);

    actorInstance.initializeAndStart(actorInstanceContext);

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.STOPPED);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getReceivedSignals()).isEmpty();
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);
  }

  @Test
  public void shouldStopOnCreationFailingActorWithDelayedSupervisionStrategy() throws InterruptedException {
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

    actorInstance.initializeAndStart(actorInstanceContext);

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
  }

  @Test
  public void shouldCreateAndRecreateStartedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>(scheduler);
    final TestActorInstance<Actor, Integer> actorInstance = new WorkingTestActorInstance(SwitchingActorSupplier.builder()
        .supplier(FailedCreationTestActor::new)
        .supplier(WorkingTestActor::new)
        .build(),
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstance.initializeAndStart(actorInstanceContext);
    assertThat(actorInstance.getPayloads()).isEmpty();

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> actorInstance.getInstanceState() == InstanceState.RUNNING);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

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

  /*
  @Test
  public void shouldCreateRestartingDelayedFailingSignalActorWithImmediateStartAndImmediateSupervision() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final SignalHandlingFailureTestActorInstance<WorkingTestActor, Integer> actorInstance = new SignalHandlingFailureTestActorInstance<>(actorInstanceContext,
        WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE,
        Signal.PRE_START);

    actorInstanceContext.assignAndCreate(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RESTARTING_DELAYED);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);
  }

  @Test
  public void shouldCreateRestartingDelayedFailingSignalActorWithImmediateStartAndImmediateSupervisionAndProcessedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final SignalHandlingFailureTestActorInstance<WorkingTestActor, Integer> actorInstance = new SignalHandlingFailureTestActorInstance<>(actorInstanceContext,
        WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE,
        Signal.PRE_START);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RESTARTING_DELAYED);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START, Signal.PRE_RESTART);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);
  }


  @Test
  public void shouldCreateDelayedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final WorkingTestActorInstance<WorkingTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.DELAYED);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.DELAYED);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).isEmpty();

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);
  }

  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndFailedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final TestActorInstance<WorkingTestActor, Integer> actorInstance = new MessageHandlingFailureTestActorInstance<>(actorInstanceContext,
        WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);
    assertThat(actorInstance.getPayloads()).isEmpty();

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, 1));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START, Signal.PRE_RESTART);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);
  }

  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndDeathNote() throws InterruptedException {
    final TestActorInstanceContext<Object> actorInstanceContext = new TestActorInstanceContext<>();
    final TestActorInstance<WorkingTestActor, Object> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);
    assertThat(actorInstance.getPayloads()).isEmpty();

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new ExtendedMessage<>(targetSlip, sourceAddress, DeathNote.INSTANCE));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START, Signal.POST_STOP);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);
  }

  @Test
  public void shouldCreateStartedStopSignalFailingActorWithImmediateStartAndImmediateSupervisionAndDeathNote() throws InterruptedException {
    final TestActorInstanceContext<Object> actorInstanceContext = new TestActorInstanceContext<>();
    final TestActorInstance<WorkingTestActor, Object> actorInstance = new SignalHandlingFailureTestActorInstance<WorkingTestActor, Object>(actorInstanceContext,
        WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE,
        Signal.POST_STOP);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);
    assertThat(actorInstance.getPayloads()).isEmpty();

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new ExtendedMessage<>(targetSlip, sourceAddress, DeathNote.INSTANCE));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START, Signal.POST_STOP);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);
  }

  @Test
  public void shouldCreateRestartingDelayedActorWithImmediateStartAndImmediateSupervisionAndDeathNote() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final WorkingTestActorInstance<FailedCreationTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        FailedCreationTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RESTARTING_DELAYED);
    assertThat(actorInstance.getReceivedSignals()).isEmpty();
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new ExtendedMessage<>(targetSlip, sourceAddress, DeathNote.INSTANCE));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.POST_STOP);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);
  }

  @Test
  public void shouldCreateRestartingDelayedFailigSignalActorWithImmediateStartAndImmediateSupervisionAndDeathNote() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final SignalHandlingFailureTestActorInstance<WorkingTestActor, Integer> actorInstance = new SignalHandlingFailureTestActorInstance<>(actorInstanceContext,
        WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE,
        Signal.PRE_START);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RESTARTING_DELAYED);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new ExtendedMessage<>(targetSlip, sourceAddress, DeathNote.INSTANCE));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START, Signal.POST_STOP);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.STOPPED);
  }

  @Test
  public void shouldCreateStartedSystemActorWithImmediateStartAndImmediateSupervisionAndIgnoredDeathNote() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final TestActorInstance<SystemTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        SystemTestActor::new,
        "system",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);
    assertThat(actorInstance.getPayloads()).isEmpty();

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new ExtendedMessage<>(targetSlip, sourceAddress, DeathNote.INSTANCE));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).hasSize(1);
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);
  }

  //--
  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndUnknownExtension() throws InterruptedException {
    final TestActorInstanceContext<Object> actorInstanceContext = new TestActorInstanceContext<>();
    final TestActorInstance<WorkingTestActor, Object> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);
    assertThat(actorInstance.getPayloads()).isEmpty();

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new ExtendedMessage<>(targetSlip, sourceAddress, new Object()));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).hasSize(1);
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);
  }

  @Test
  public void shouldCreateRestartingDelayedActorWithImmediateStartAndImmediateSupervisionAndUnknownExtension() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final WorkingTestActorInstance<FailedCreationTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        FailedCreationTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RESTARTING_DELAYED);
    assertThat(actorInstance.getReceivedSignals()).isEmpty();
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new ExtendedMessage<>(targetSlip, sourceAddress, new Object()));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RESTARTING_DELAYED);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).isEmpty();
    assertThat(actorInstanceContext.getUndeliverableMessages()).hasSize(1);
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);
  }

  @Test
  public void shouldCreateRestartingDelayedFailigSignalActorWithImmediateStartAndImmediateSupervisionAndUnknownExtension() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final SignalHandlingFailureTestActorInstance<WorkingTestActor, Integer> actorInstance = new SignalHandlingFailureTestActorInstance<>(actorInstanceContext,
        WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE,
        Signal.PRE_START);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RESTARTING_DELAYED);
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new ExtendedMessage<>(targetSlip, sourceAddress, new Object()));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RESTARTING_DELAYED);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).hasSize(1);
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);
  }

  @Test
  public void shouldCreateStartedSystemActorWithImmediateStartAndImmediateSupervisionAndUnknownExtension() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final TestActorInstance<SystemTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        SystemTestActor::new,
        "system",
        new ImmediateRestartSupervisionStrategy(1),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndCreate(actorInstance);
    assertThat(actorInstance.getPayloads()).isEmpty();

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new ExtendedMessage<>(targetSlip, sourceAddress, new Object()));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).hasSize(1);
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);
  }

*/

  @RequiredArgsConstructor
  @Getter
  private static class MailboxHolder<T> {
    private final Lock processingLock = new ReentrantLock();
    private final Mailbox<T> mailbox;
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

    private Mailbox<Message<T>> mailbox;

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