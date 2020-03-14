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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ActorInstanceTest {

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

  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final TestActorInstance<WorkingTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        WorkingTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndStart(actorInstance);
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
    assertThat(actorInstance.getReceivedSignals()).containsExactly(Signal.PRE_START);
    assertThat(actorInstanceContext.getUndeliverableMessages()).isEmpty();
    assertThat(actorInstanceContext.getInstanceState())
        .isEqualTo(TestActorInstanceContext.TestInstanceState.ACTIVE);
  }

  @Test
  public void shouldCreateRestartingDelayedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final WorkingTestActorInstance<FailedCreationTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        FailedCreationTestActor::new,
        "test",
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.IMMEDIATE);

    actorInstanceContext.assignAndStart(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RESTARTING_DELAYED);
    assertThat(actorInstance.getReceivedSignals()).isEmpty();
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
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.DELAYED);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndStart(actorInstance);

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
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndStart(actorInstance);
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
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndStart(actorInstance);
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
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndStart(actorInstance);

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
  public void shouldCreateStartedSystemActorWithImmediateStartAndImmediateSupervisionAndIgnoredDeathNote() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final TestActorInstance<SystemTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        SystemTestActor::new,
        "system",
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndStart(actorInstance);
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


  @RequiredArgsConstructor
  @Getter
  private static class MailboxHolder<T> {
    private final Lock processingLock = new ReentrantLock();
    private final Mailbox<T> mailbox;
  }

  @RequiredArgsConstructor
  private static final class TestActorInstanceContext<T> implements ActorInstanceContext<T> {
    public enum TestInstanceState {
      ACTIVE,
      STOPPED;
    }

    private final MailboxHolder<Message<T>> mailboxHolder = new MailboxHolder<>(new UnboundedMailbox<>());
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Getter
    private final List<Message<T>> undeliverableMessages = new LinkedList<>();

    @Getter
    private TestInstanceState instanceState = TestInstanceState.ACTIVE;

    private ActorInstance<? extends Actor, T> actorInstance;

    @Override
    public CompletionStage<Void> runAsync(Runnable runnable) {
      return CompletableFuture.runAsync(runnable, executorService);
    }

    @Override
    public <V> CompletionStage<V> submitAsync(Supplier<V> supplier) {
      return CompletableFuture.supplyAsync(supplier, executorService);
    }

    @Override
    public void scheduleMessageProcessing() {
      if (mailboxHolder.getProcessingLock().tryLock()) {
        try {
          CompletableFuture.runAsync(() -> mailboxHolder
              .getMailbox()
              .takeMessage()
              .ifPresent(message -> actorInstance.handleNextMessage(message)), executorService)
              .whenComplete((s, t) -> {
                final boolean needReschedule = mailboxHolder.getMailbox().needsScheduling();

                if (needReschedule) {
                  scheduleMessageProcessing();
                }
              });
        } finally {
          mailboxHolder.getProcessingLock().unlock();
        }
      }
    }

    @Override
    public void enqueueMessage(Message<T> message) {
      mailboxHolder.getMailbox().putMessage(message);
    }

    @Override
    public void undeliverableMessage(Message<T> message) {
      undeliverableMessages.add(message);
    }

    @Override
    public <V extends Actor> void assignAndStart(ActorInstance<V, T> actorInstance) {
      this.actorInstance = actorInstance;

      actorInstance.transitionState(InstanceState.RUNNING);
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
    public void actorInstanceStopped() {
      this.instanceState = TestInstanceState.STOPPED;
    }
  }

  private static class TestActorInstance<V extends Actor, T> extends ActorInstance<V, T> {
    @Getter
    private List<Signal> receivedSignals = new LinkedList<>();

    @Getter
    private final List<T> payloads = Collections.synchronizedList(new LinkedList<>());

    protected TestActorInstance(ActorInstanceContext context, Callable<V> supplier, String name, SupervisionStrategyInternal supervisionStrategy, StartupMode startupMode) {
      super(context, supplier, name, supervisionStrategy, startupMode);
    }

    @Override
    protected final void handleMessage(final Message<T> message) {
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

    public WorkingTestActorInstance(ActorInstanceContext<T> context,
                                    Callable<V> supplier,
                                    String name,
                                    SupervisionStrategyInternal supervisionStrategy,
                                    StartupMode startupMode) {
      super(context, supplier, name, supervisionStrategy, startupMode);
    }
  }

  private static class MessageHandlingFailureTestActorInstance<V extends Actor, T> extends TestActorInstance<V, T> {
    public MessageHandlingFailureTestActorInstance(ActorInstanceContext<T> context,
                                                   Callable<V> supplier,
                                                   String name,
                                                   SupervisionStrategyInternal supervisionStrategy,
                                                   StartupMode startupMode) {
      super(context, supplier, name, supervisionStrategy, startupMode);
    }

    @Override
    protected void handleMessageInternal(Message<T> message) {
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

  private static class SystemTestActor implements Actor, SystemActor {
    @Getter
    private ActorContext context;

    @Override
    public void setupContext(ActorContext context) {
      this.context = context;
    }
  }
}