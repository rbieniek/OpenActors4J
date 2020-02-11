package io.openactors4j.core.impl.common;

import static org.assertj.core.api.Assertions.assertThat;


import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.NotImplementedException;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.common.SystemAddress;
import io.openactors4j.core.common.UnboundedMailbox;
import io.openactors4j.core.impl.messaging.Message;
import io.openactors4j.core.impl.messaging.RoutingSlip;
import io.openactors4j.core.impl.messaging.SystemAddressImpl;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
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
  public void shouldCreateStartedActorWithImmediateStartAndImmediateSupervision() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final WorkingTestActorInstance<Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
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
    assertThat(actorInstance.isStarted()).isTrue();
    assertThat(actorInstance.getPayloads()).containsExactly(1);
  }

  @Test
  public void shouldCreateRestartingDelayedActorWithImmediateStartAndImmediateSupervision() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final StartupFailingTestActorInstance<Integer> actorInstance = new StartupFailingTestActorInstance<>(actorInstanceContext,
        "test",
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.IMMEDIATE);

    actorInstanceContext.assignAndStart(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RESTARTING_DELAYED);
  }

  @Test
  public void shouldCreateDelayedActorWithImmediateStartAndImmediateSupervision() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final WorkingTestActorInstance<Integer> actorInstance = new WorkingTestActorInstance<>(actorInstanceContext,
        "test",
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.DELAYED);
    final RoutingSlip targetSlip = new RoutingSlip(testAddress);

    actorInstanceContext.assignAndStart(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.DELAYED);
    assertThat(actorInstance.isStarted()).isFalse();
    assertThat(actorInstance.getPayloads()).isEmpty();

    targetSlip.nextPathPart(); // skip over path part '/test' to complete routing in tested actor instance
    actorInstance.routeMessage(new Message<>(targetSlip, sourceAddress, Integer.valueOf(1)));

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.isStarted()).isTrue();
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
    @Override
    public CompletionStage<Void> runAsync(Runnable runnable) {
      return CompletableFuture.runAsync(runnable, executorService);
    }

    @Override
    public <V> CompletionStage<V> submitAsync(Supplier<V> supplier) {
      return CompletableFuture.supplyAsync(supplier, executorService);
    }

    private final MailboxHolder<Message<T>> mailboxHolder = new MailboxHolder<>(new UnboundedMailbox<>());
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Getter
    private final List<Message<T>> undeliverableMessages = new LinkedList<>();

    private ActorInstance<T> actorInstance;

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
    public void assignAndStart(ActorInstance<T> actorInstance) {
      this.actorInstance = actorInstance;

      actorInstance.transitionState(InstanceState.RUNNING);
    }

    @Override
    public ActorInstance parentActor() {
      throw new NotImplementedException();
    }
  }

  private static class WorkingTestActorInstance<T> extends ActorInstance<T> {
    @Getter
    private final List<T> payloads = new LinkedList<>();

    @Getter
    private boolean started = false;

    public WorkingTestActorInstance(ActorInstanceContext<T> context, String name,
                                    SupervisionStrategyInternal supervisionStrategy,
                                    StartupMode startupMode) {
      super(context, name, supervisionStrategy, startupMode);
    }

    @Override
    protected void handleMessage(Message<T> message) {
      payloads.add(message.getPayload());
    }

    @Override
    protected void startInstance() {
      started = true;
    }
  }

  private static class StartupFailingTestActorInstance<T> extends ActorInstance<T> {
    @Getter
    private final List<T> payloads = new LinkedList<>();

    public StartupFailingTestActorInstance(ActorInstanceContext<T> context, String name,
                                           SupervisionStrategyInternal supervisionStrategy,
                                           StartupMode startupMode) {
      super(context, name, supervisionStrategy, startupMode);
    }

    @Override
    protected void handleMessage(Message<T> message) {
      payloads.add(message.getPayload());
    }

    @Override
    protected void startInstance() {
      throw new IllegalArgumentException();
    }
  }
}