package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.impl.messaging.Message;

public interface MailboxScheduler<T> {
  FlowControlledMailbox<Message<T>> registerMailbox(Mailbox<Message<T>> mailbox, MailboxSchedulerClient client);

  void deregisterMailbox(Mailbox<Message<T>> mailbox);
}
