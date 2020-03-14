package io.openactors4j.core.impl.common;

/**
 * Tag interface to mark actor implementations for special handling
 *
 * <ul>
 *   <li>A system actor ignores the common {@link io.openactors4j.core.common.DeathNote} message
 *   because this user-level message is not supposed to be used to terminate the actor system
 *   </li>
 * </ul>
 */
public interface SystemActor {
}
