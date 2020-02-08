package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.impl.messaging.Message;
import io.openactors4j.core.impl.system.SupervisionStrategyInternal;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

class ActorInstanceTest {

  @Test
  public void shouldCreateActorWithImmediateStartAndImmediateSupervision() {

  }

  @RequiredArgsConstructor
  private static final class TestActorInstanceContext<T> implements ActorInstanceContext {
    private final Mailbox<Message<T>> mailbox;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Set<Mailbox<Message<T>>> scheduledMailboxes = new ConcurrentSkipListSet<>();

    @Override
    public void scheduleMessageProcessing() {

    }

    @Override
    public void enqueueMessage(Message message) {
      mailbox.putMessage(message);
    }

    @Override
    public void undeliverableMessage(Message message) {

    }

    @Override
    public ActorInstance parentActor() {
      return null;
    }
  }

  private static class TestActorInstance extends ActorInstance<Object> {
    protected TestActorInstance(ActorInstanceContext context, String name,
                                SupervisionStrategyInternal supervisionStrategy,
                                StartupMode startupMode) {
      super(context, name, supervisionStrategy, startupMode);
    }

    @Override
    protected void handleMessage(Message<Object> message) {

    }

    @Override
    protected void startInstance() {

    }
  }
}