package io.openactors4j.core.typed;

import io.openactors4j.core.common.ActorRef;
import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.StartupMode;
import io.openactors4j.core.common.SupervisionStrategy;
import java.util.function.Function;

@SuppressWarnings("PMD.TooManyMethods")
public interface BehaviorBuilder<T> {

  /**
   * The {@link SupervisionStrategy} to be executed on the created instance if the message
   * processing raises an {@link Exception}.
   * <p>
   * This strategy is not executed when an {@link Error} is raised. The abnormal condition signaled
   * by the error can normally not coped with anyway, other than simply terminating the program
   * anyway.
   *
   * @param strategy the supervision strategy to be used
   * @return this builder instance
   */
  BehaviorBuilder<T> withSupervisionStrategy(SupervisionStrategy strategy);

  /**
   * The {@link Mailbox} to be used for this actor.
   * <p>
   * If this method is not used on the builder, it defaults
   * to {@link io.openactors4j.core.common.UnboundedMailbox}
   * </p>
   *
   * @param mailbox the {@link Mailbox} implementation to be used
   * @return this builder instance
   */
  BehaviorBuilder<T> withMailbox(Mailbox<Object> mailbox);

  /**
   * Set the name of the actor to be created.
   *
   * <B>Please note:</B> This name is absolute and trying to create a actor with a name which
   * already exists in the current scope, either {@link io.openactors4j.core.common.ActorSystem})
   * or {@link io.openactors4j.core.common.ActorContext}, will cause the creation to fail
   *
   * @param actorName the actor name
   * @return this builder instance
   */
  BehaviorBuilder<T> withAbsoluteName(String actorName);

  /**
   * Set the name prefix for the created actor.
   * <p>
   * The actor name will be constructed from this prefix and an unique numeric ID value
   * </p>
   *
   * @param actorNamePrefix the prefix part of the unique actor name
   * @return this builder instance
   */
  BehaviorBuilder<T> withNamePrefix(String actorNamePrefix);

  /**
   * Set the {@link StartupMode} for the actor.
   * <p>
   * The default behavior is to start the actor immediately on creation by using the default
   * value {@link StartupMode#IMMEDIATE}
   * </p>
   *
   * @param startupMode
   * @return this builder instance
   */
  BehaviorBuilder<T> withStartupMode(StartupMode startupMode);

  /**
   * Add a specific behavior function for message type
   *
   * @param message message to be handled
   * @param handler the message handler function
   * @return this builder instances
   */
  BehaviorBuilder<T> withMessage(T message, Function<T, Behavior<T>> handler);

  /**
   * Add a handler for the {@link io.openactors4j.core.common.Signal#PRE_START} lifecycle signal
   *
   * @param signalHandler the signal handler
   * @return this builder instance
   */
  BehaviorBuilder<T> onPreStart(Behavior<T> signalHandler);

  /**
   * Add a handler for the {@link io.openactors4j.core.common.Signal#PRE_RESTART} lifecycle signal
   *
   * @param signalHandler the signal handler
   * @return this builder instance
   */
  BehaviorBuilder<T> onPreRestart(Behavior<T> signalHandler);

  /**
   * Add a handler for the {@link io.openactors4j.core.common.Signal#POST_STOP} lifecycle signal
   *
   * @param signalHandler the signal handler
   * @return this builder instance
   */
  BehaviorBuilder<T> onPostStop(Behavior<T> signalHandler);

  /**
   * Create the actor instance based upon the parametrization given by this build instance
   *
   * @return a reference to the created actor
   */
  ActorRef create();

  /**
   * Return an identity behavior
   *
   * @return an identity behavior
   */
  Behavior<T> same();

  /**
   * Create a behavior which causes the actor to stop
   *
   * @return a behavior which causes the actor to stop
   */
  Behavior<T> stopped();
}
