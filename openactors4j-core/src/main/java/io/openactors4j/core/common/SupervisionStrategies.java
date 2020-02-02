package io.openactors4j.core.common;

import java.time.Duration;

/**
 * Producer interface for the supervision strategies supported
 */
public interface SupervisionStrategies {
  /**
   * Access a supervision strategy to restart the actor immediately after it crashes
   *
   * @return the supervision strategy
   */
  SupervisionStrategy restart();

  /**
   * Access a supervision strategy to terminate the actor and remove it from the
   * actor system immediately after it crashes
   *
   * <p>
   * <b>Please note:</b> Any subsequent message queued for or sent to the actor will be processed
   * but instead handed over to the actor systems unreachable handler
   * </p>
   *
   * @return the supervision strategy
   */
  SupervisionStrategy terminate();

  /**
   * Access a supervision strategy which passes the crash handling up the chain to the parent actor
   *
   * @return the supervision strategy
   */
  SupervisionStrategy bubbleUp();

  /**
   * Access a supervision strategy to schedule the future restart the actor after it crashes
   *
   * In case the actor restart fails, the actor restart is attempted again after the
   * given restart period
   *
   * <p>
   *   <b>Please note:</b> the restart period is the duration within which the actor is blocked
   *   from a restart attempt. The actor system may delay the restart further, depending on system
   *   conditions
   * </p>
   * @param restartPeriod the minimum duration before the actor restart is attempted
   * @return the supervision strategy
   */
  SupervisionStrategy delayedRestart(Duration restartPeriod);

  /**
   * Access a supervision strategy to schedule the future restart the actor after it crashes.
   *
   * In case the actor restart fails, the actor restart is attempted again after the
   * given restart period extended by the backoff period for any subsequent retry
   *
   * <p>
   *   <b>Please note:</b> the restart period is the duration within which the actor is blocked
   *   from a restart attempt. The actor system may delay the restart further, depending on system
   *   conditions
   * </p>
   * @param restartPeriod the minimum duration before the actor restart is attempted
   * @param backoffPeriod this period is added to the restart period in case the actor
   *                      restart fails and another attempt needs to be scheduled
   * @return the supervision strategy
   */
  SupervisionStrategy delayedRestart(Duration restartPeriod, Duration backoffPeriod);

  /**
   * Access a supervision strategy to schedule the future restart the actor after it crashes.
   *
   * In case the actor restart fails, the actor restart is attempted again after the
   * given restart period extended by the backoff period for any subsequent retry
   *
   * <p>
   *   <b>Please note:</b> the restart period is the duration within which the actor is blocked
   *   from a restart attempt. The actor system may delay the restart further, depending on system
   *   conditions
   * </p>
   * @param restartPeriod the minimum duration before the actor restart is attempted
   * @param backoffPeriod this period is added to the restart period in case the actor
   *                      restart fails and another attempt needs to be scheduled
   * @param backoffFactor a liner factor applied to the backoff period on subsequent retry attempts
   * @return the supervision strategy
   */
  SupervisionStrategy delayedRestart(Duration restartPeriod, Duration backoffPeriod, int backoffFactor);
}
