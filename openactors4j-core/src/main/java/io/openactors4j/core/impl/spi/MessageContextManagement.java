package io.openactors4j.core.impl.spi;

import io.openactors4j.core.spi.MessageContextManager;
import io.openactors4j.core.spi.MessageContextProvider;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageContextManagement {
  private MessageContextProvider provider;

  private MessageContextManager manager;
}
