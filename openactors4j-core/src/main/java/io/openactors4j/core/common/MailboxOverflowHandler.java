package io.openactors4j.core.common;

/**
 * This interface defines a framework component provided by the {@link ActorSystem} to cope
 * with messages that cannot be placed into a mailbox due to restrictions imposed by the
 * mailbox implementation itself
 *
 * @param <T> the message type handled by this overflow handler
 */
public interface MailboxOverflowHandler<T> {
  /**
   * Pass the message to {@link ActorSystem} to deal with the overflow situation as defined
   * per system-wide policy
   *
   * @param message the message being rejected
   */
  void messageOverflow(T message);
}
