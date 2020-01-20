package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.ActorSystem;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUnrecoverableErrorHandler implements Consumer<Throwable> {
  private static final Logger logger = LoggerFactory.getLogger(ActorSystem.class);

  @Override
  public void accept(Throwable throwable) {
    logger.error("Unrecoverable error", throwable);
  }
}
