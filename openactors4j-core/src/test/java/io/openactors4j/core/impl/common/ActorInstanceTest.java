package io.openactors4j.core.impl.common;

import static org.assertj.core.api.Assertions.assertThat;


import io.openactors4j.core.common.Actor;
import io.openactors4j.core.common.ActorContext;
import io.openactors4j.core.common.ActorRef;
import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.NotImplementedException;
import io.openactors4j.core.common.Signal;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.common.SupervisionStrategies;
import io.openactors4j.core.common.SystemAddress;
import io.openactors4j.core.common.UnboundedMailbox;
import io.openactors4j.core.impl.messaging.Message;
import io.openactors4j.core.impl.messaging.RoutingSlip;
import io.openactors4j.core.impl.messaging.SystemAddressImpl;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import io.openactors4j.core.typed.BehaviorBuilder;
import io.openactors4j.core.untyped.UntypedActorBuilder;
import java.util.LinkedList;
import java.util.List;
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
        () -> new WorkingTestActor(),
        "test",
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndStart(actorInstance);
    assertThat(actorInstance.getPayloads()).isEmpty();

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, Integer.valueOf(1)));


    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
  }

  @Test
  public void shouldCreateRestartingDelayedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final WorkingTestActorInstance<FailingTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        () -> new FailingTestActor(),
        "test",
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.IMMEDIATE);

    actorInstanceContext.assignAndStart(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RESTARTING_DELAYED);
  }

  @Test
  public void shouldCreateDelayedActorWithImmediateStartAndImmediateSupervisionAndProcessedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final WorkingTestActorInstance<WorkingTestActor, Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        () -> new WorkingTestActor(),
        "test",
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.DELAYED);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndStart(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.DELAYED);
    assertThat(actorInstance.getPayloads()).isEmpty();

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, Integer.valueOf(1)));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
  }

  @Test
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervisionAndFailedMessage() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final MessageHandlingFailureTestActorInstance<WorkingTestActor, Integer> actorInstance = new MessageHandlingFailureTestActorInstance<>(actorInstanceContext,
        () -> new WorkingTestActor(),
        "test",
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.IMMEDIATE);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndStart(actorInstance);
    assertThat(actorInstance.getPayloads()).isEmpty();

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).isEmpty();

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, Integer.valueOf(1)));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.getPayloads()).containsExactly(1);
  }

  @RequiredArgsConstructor
  @Getter
  private static class MailboxHolder<T> {
    private final Lock processingLock = new ReentrantLock();
    private final Mailbox<T> mailbox;
  }

  @RequiredArgsConstructor
  private static final class TestActorInstanceContext<T> implements ActorInstanceContext<T> {
    private final MailboxHolder<Message<T>> mailboxHolder = new MailboxHolder<>(new UnboundedMailbox<>());
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Getter
    private final List<Message<T>> undeliverableMessages = new LinkedList<>();

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
        CompletableFuture.runAsync(() -> mailboxHolder
            .getMailbox()
            .takeMessage()
            .ifPresent(message -> actorInstance.handleNextMessage(message)), executorService)
            .whenComplete((s, t) -> {
              final boolean needReschedule = mailboxHolder.getMailbox().needsScheduling();

              mailboxHolder.getProcessingLock().unlock();

              if (needReschedule) {
                scheduleMessageProcessing();
              }
            });
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
  }

  private static abstract class TestActorInstance<V extends Actor, T> extends ActorInstance<V, T> {
    @Getter
    protected List<Signal> receivedSignals = new LinkedList<>();

    @Getter
    protected final List<T> payloads = new LinkedList<>();

    protected TestActorInstance(ActorInstanceContext context, Supplier<V> supplier, String name, SupervisionStrategyInternal supervisionStrategy, StartupMode startupMode) {
      super(context, supplier, name, supervisionStrategy, startupMode);
    }

    @Override
    protected void handleMessage(Message<T> message) {
      payloads.add(message.getPayload());
    }

    @Override
    protected void sendSignal(Signal signal) {
      receivedSignals.add(signal);
    }
  }

  private static class WorkingTestActorInstance<V extends Actor, T> extends TestActorInstance<V, T> {

    public WorkingTestActorInstance(ActorInstanceContext<T> context, Supplier<V> supplier,
                                    String name,
                                    SupervisionStrategyInternal supervisionStrategy,
                                    StartupMode startupMode) {
      super(context, supplier, name, supervisionStrategy, startupMode);
    }
  }

  private static class StartupFailingTestActorInstance<V extends Actor, T> extends TestActorInstance<V, T> {
    public StartupFailingTestActorInstance(ActorInstanceContext<T> context,
                                           Supplier<V> supplier,
                                           String name,
                                           SupervisionStrategyInternal supervisionStrategy,
                                           StartupMode startupMode) {
      super(context, supplier, name, supervisionStrategy, startupMode);


    }
  }

  private static class MessageHandlingFailureTestActorInstance<V extends Actor, T> extends TestActorInstance<V, T> {
    public MessageHandlingFailureTestActorInstance(ActorInstanceContext<T> context,
                                                   Supplier<V> supplier,
                                                   String name,
                                                   SupervisionStrategyInternal supervisionStrategy,
                                                   StartupMode startupMode) {
      super(context, supplier, name, supervisionStrategy, startupMode);
    }

    @Override
    protected void handleMessage(Message<T> message) {
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

  private static class FailingTestActor implements Actor {
    @Getter
    private ActorContext context;

    private FailingTestActor() {
      throw new IllegalArgumentException();
    }

    @Override
    public void setupContext(ActorContext context) {
      this.context = context;
    }
  }
}