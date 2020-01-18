package io.openactors4j.core.common;

/**
 * This exception is raised in the context of methods which have (purposely) chosen to be
 * left unimplemented, never to be called from application code anyway.
 */
public class NotImplementedException extends RuntimeException {
  private static final int serialVersionUID = 2020011701;
}
