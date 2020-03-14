package io.openactors4j.core.impl.messaging;

import io.openactors4j.core.common.SystemAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * This container class encapsulates a message sent by the public API
 *
 * @param <T> the message payload type
 */
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
@Getter
@ToString
public class Message<T> {
  private final RoutingSlip target;
  private final SystemAddress sender;
  private final T payload;
  private final Map<String, Object> messageContext = new ConcurrentHashMap<>();
}
