package io.openactors4j.core.common;

import java.util.Optional;

/**
 * The mailbox abstraction is a central piece of the overall message delivery system for
 * communicating with an actor instance.
 *
 * Messages are never sent directly to an actor. Instead, the messages are buffered in a mailbox
 * awaiting processing by an actor whenever the scheduler can assign resources.
 *
 * @param <T> message type assigned to the mailbox implementation
 */
public interface Mailbox<T> {

  /**
   * lifecylce method to assign the overflow handler to the mailbox.
   *
   * An implementation may choose to not support message overflow at all, for example if it is
   * an implementation without any restrictions on the size of the managed mailbox
   *
   * @param handler the overflow handler provided by the {@link ActorSystem}
   */
  default void setOverflowHandler(MailboxOverflowHandler<T> handler) {};

  /**
   * lifecycle method to notify the mailbox implementation that it should begin to accept messages
   *
   * If an implementation supports this lifecycle method, it must hand over any messages to the
   * {@link Mailbox#setOverflowHandler(MailboxOverflowHandler)} before this lifecycle method was called
   */
  default void startReceiving() {};

  /**
   * lifecycle method to notify the mailbox implementation that it should stop to accept messages.
   *
   * If an implementation supports this lifecycle method, it must hand over any messages to the
   * {@link Mailbox#setOverflowHandler(MailboxOverflowHandler)} after this lifecycle method was called
   */
  default void stopReceiving() {};

  /**
   * Query the mailbox if it has at least one message which needs processing
   *
   * @return {@code true} if the mailbox has at least on message ready for processing,
   * {@code false} otherwise
   */
  boolean needsScheduling();

  /**
   * Insert a message into this mailbox
   *
   * @param message the message to put into this mailbox
   */
  void putMessage(T message);

  /**
   * Take the next messsage, if any, from the mailbox
   *
   * @return An {@link Optional} with the mext message to be taken if any. The {@link Optional}
   * is empty if there is no message to be taken
   */
  Optional<T> takeMessage();

}
