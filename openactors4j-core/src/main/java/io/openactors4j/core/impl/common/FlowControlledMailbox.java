package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Mailbox;

public interface FlowControlledMailbox<T> extends Mailbox<T> {
  enum ProcessMode {
    DELIVER,
    QUEUE;
  }

  void processMode(ProcessMode mode);
}
