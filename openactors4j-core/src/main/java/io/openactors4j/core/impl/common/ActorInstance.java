package io.openactors4j.core.impl.common;

import static java.util.Optional.ofNullable;
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

  /**
   * Route the incoming message according to its path:
   * <ul>
   * <li>if the target routing slip has q child path, route the message to the child actor with the
   * name denoted by next path part. If no child actor with a matching name can be found, send the
   * message to the systems unreachable handler</li>
   * <li>if the message if targeted to this actor, enqueue the message into the mailbox</li>
   * </ul>
   *
   * @param message the message to be routed
   */
  public void routeMessage(final Message<T> message) {
    final Optional<String> currentPart = message.getTarget().nextPathPart();

    currentPart.ifPresentOrElse(pathPath -> {
      ofNullable(childActors.get(pathPath))
          .ifPresentOrElse(child -> child.routeMessage(message),
              () -> context.undeliverableMessage(message));
    }, () -> {
      mailbox.putMessage(message);
      context.scheduleMessageProcessing();
    });
  }

  /**
   * handle the next message in the mailbox
   */
  public void handleNextMessage() {
    mailbox.takeMessage().ifPresent(message -> {
      try {
        handleMessage(message);
      } catch (Exception e) {

      }
    });
  }

  /**
   * Handle a message which is destined for this actor
   *
   * @param message the message to process;
   */
  protected abstract void handleMessage(final Message<T> message);

  /**
   *
   */
  protected abstract void startInstance();

  /**
   * obtain the current instance status
   */
  protected abstract InstanceState instanceState();
}
