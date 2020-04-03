package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.MailboxOverflowHandler;
import io.openactors4j.core.impl.messaging.Message;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ReactiveMailboxScheduler<T> implements MailboxScheduler<T> {
  private final ExecutorService executorService;

  private Map<UUID, WrappedMailbox> mailboxes = new ConcurrentHashMap<>();
  private SubmissionPublisher<UUID> eventPublisher;
  private SchedulingSubscriber eventSubscriber;

  public void initialize() {
    eventPublisher = new SubmissionPublisher<UUID>(executorService, Flow.defaultBufferSize());

    eventSubscriber = new SchedulingSubscriber(mailboxes);
    eventPublisher.subscribe(eventSubscriber);
  }

  @Override
  public FlowControlledMailbox<Message<T>> registerMailbox(Mailbox<Message<T>> mailbox, MailboxSchedulerClient client) {
    final UUID uuid = UUID.randomUUID();
    final WrappedMailbox<T> wrappedMailbox = new WrappedMailbox<>(eventPublisher,
        new SubmissionPublisher<>(executorService, Flow.defaultBufferSize()),
        mailbox,
        uuid,
        client);

    mailboxes.put(uuid, wrappedMailbox);

    return wrappedMailbox;
  }

  @Override
  public void deregisterMailbox(Mailbox<Message<T>> mailbox) {
    if (mailbox instanceof WrappedMailbox) {
      mailboxes.remove(((WrappedMailbox<T>) mailbox).getUuid());
    }
  }

  @RequiredArgsConstructor
  private static class SchedulingSubscriber implements Flow.Subscriber<UUID> {
    private final Map<UUID, WrappedMailbox> mailboxes;

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
      this.subscription = subscription;

      subscription.request(1);
    }

    @Override
    public void onNext(UUID item) {
      Optional.ofNullable(mailboxes.get(item))
          .ifPresent(mailbox -> mailbox.getMailboxPublisher().submit(item));

      subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
      log.error("Caught execption while processing schedule request", throwable);
    }

    @Override
    public void onComplete() {

    }
  }

  @RequiredArgsConstructor
  private static class WrappedMailbox<T> implements FlowControlledMailbox<Message<T>> {
    private final SubmissionPublisher<UUID> schedulePublisher;

    @Getter
    private final SubmissionPublisher<UUID> mailboxPublisher;
    private final Mailbox<Message<T>> delegate;

    @Getter
    private final UUID uuid;
    private final MailboxSchedulerClient client;

    private MailboxSchedulerSubscriber clientSubscriber;
    private FlowControlledMailbox.ProcessMode flowMode = ProcessMode.DELIVER;
    private Queue<UUID> backlog = new ConcurrentLinkedQueue<>();

    @Override
    public void startReceiving() {
      delegate.startReceiving();

      clientSubscriber = new MailboxSchedulerSubscriber(client);
      mailboxPublisher.subscribe(clientSubscriber);
    }

    @Override
    public void stopReceiving() {
      delegate.stopReceiving();

      mailboxPublisher.close();
    }

    @Override
    public void setOverflowHandler(MailboxOverflowHandler<Message<T>> handler) {
      delegate.setOverflowHandler(handler);
    }

    @Override
    public boolean needsScheduling() {
      return delegate.needsScheduling();
    }

    @Override
    public void putMessage(Message<T> message) {
      delegate.putMessage(message);

      if(flowMode == ProcessMode.DELIVER) {
        schedulePublisher.submit(uuid);
      } else {
        backlog.add(uuid);
      }
    }

    @Override
    public Optional<Message<T>> takeMessage() {
      return delegate.takeMessage();
    }

    @Override
    public void processMode(ProcessMode processMode) {
      this.flowMode = processMode;
      clientSubscriber.processMode(processMode);

      if(processMode == ProcessMode.DELIVER) {
        UUID pollValue;

        while((pollValue = backlog.poll()) != null) {
          schedulePublisher.submit(pollValue);
        }
      }
    }
  }

  @RequiredArgsConstructor
  private static class MailboxSchedulerSubscriber implements Flow.Subscriber<UUID> {
    private final MailboxSchedulerClient client;

    private Flow.Subscription subscription;
    private FlowControlledMailbox.ProcessMode flowMode = FlowControlledMailbox.ProcessMode.DELIVER;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
      this.subscription = subscription;

      subscription.request(1);
    }

    @Override
    public void onNext(UUID item) {
      if(flowMode == FlowControlledMailbox.ProcessMode.DELIVER) {
        client.takeNextMessage();
      }

      subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
      log.error("Failed to deliver message to client", throwable);
    }

    @Override
    public void onComplete() {

    }

    public void processMode(FlowControlledMailbox.ProcessMode processMode) {
      this.flowMode = processMode;
    }
  }
}
