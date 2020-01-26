package io.openactors4j.core.impl.common;

import static lombok.AccessLevel.PROTECTED;


import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.SupervisionStrategy;
import io.openactors4j.core.impl.messaging.Message;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This class models the actual actor instance held in the tree of actors inside the
 * actor system
 */
@RequiredArgsConstructor(access = PROTECTED)
@Getter(PROTECTED)
public abstract class ActorInstance<T> {

  private final ActorInstanceContext context;
  private final String name;
  private final SupervisionStrategy supervisionStrategy;
  private final Mailbox<Message<T>> mailbox;

  private final Map<String, ActorInstance> childActors = new ConcurrentHashMap<>();

  public void routeMessage(final Message<T> message) {
    final Optional<String> currentPart = message.getTarget().nextPathPart();

    currentPart.ifPresentOrElse(pathPath -> {
      Optional.ofNullable(childActors.get(pathPath))
          .ifPresentOrElse(child -> child.routeMessage(message),
              () -> context.undeliverableMessage(message));
    }, () -> {
      mailbox.putMessage(message);
      context.scheduleMessageProcessing();
    });
  }

  public void handleNextMessage() {
    mailbox.takeMessage().ifPresent(message -> {

    });
  }

  /**
   * Handle a message which is destined for this actor
   *
   * @param message the message to process;
   */
  protected abstract void handleMessage(final Message<T> message);
}
