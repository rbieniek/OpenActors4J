package io.openactors4j.core.impl.common;

import static org.assertj.core.api.Assertions.assertThat;


import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.NotImplementedException;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.common.UnboundedMailbox;
import io.openactors4j.core.impl.messaging.Message;
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
import org.junit.jupiter.api.Test;

public class ActorInstanceTest {

  @Test
  public void shouldCreateActorWithImmediateStartAndImmediateSupervision() throws InterruptedException {
    final TestActorInstanceContext<Integer> actorInstanceContext = new TestActorInstanceContext<>();
    final TestActorInstance<Integer> actorInstance = new TestActorInstance<>(actorInstanceContext,
        "test",
        new ImmediateRestartSupervisionStrategy(),
        StartupMode.IMMEDIATE);

    actorInstanceContext.assignAndStart(actorInstance);

    Thread.sleep(100);
    assertThat(actorInstance.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(actorInstance.isStarted()).isTrue();
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
            .ifPresent(message -> actorInstance.handleMessage(message)), executorService)
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

  private static class TestActorInstance<T> extends ActorInstance<T> {
    @Getter
    private final List<T> payloads = new LinkedList<>();

    @Getter
    private boolean started = false;

    public TestActorInstance(ActorInstanceContext<T> context, String name,
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
}