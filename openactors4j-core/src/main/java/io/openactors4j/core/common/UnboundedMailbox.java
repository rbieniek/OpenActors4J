package io.openactors4j.core.common;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Implementation of the {@link Mailbox} interface.
 *
 * This implementation does not impose any restrictions on the mailbox size att
 *
 * @param <T> the type of the messages contained in this mailbox implementation
 */
public class UnboundedMailbox<T> implements Mailbox<T> {
  private MailboxOverflowHandler<T> overflowHandler;

  @Override
  public void overflowHandler(MailboxOverflowHandler<T> overflowHandler) {
    this.overflowHandler = overflowHandler;
  }

  private ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

  @Override
  public boolean needsScheduling() {
    return !queue.isEmpty();
  }

  @Override
  public void putMessage(T message) {
    if(!queue.offer(message)) {
      // the queue is unbounded, so this should never happen.
      overflowHandler.messageOverflow(message);
    }
  }

  @Override
  public Optional<T> takeMessage() {
    return Optional.ofNullable(queue.poll());
  }
}