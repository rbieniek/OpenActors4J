package io.openactors4j.core.impl.messaging;

import io.openactors4j.core.common.SystemAddress;
import lombok.Getter;
import lombok.ToString;

/**
 * Type-safe extension of {@link Message} to carry extended data.
 * <p>
 * This message type is used to carry special messages like
 * {@link io.openactors4j.core.common.DeathNote} in a type-safe manner
 *
 * @param <T> the message payload type
 * @param <V> the extended data payload
 */
@Getter
@ToString(callSuper = true)
public class ExtendedMessage<T, V> extends Message<T> {
  private final V extensionData;

  public ExtendedMessage(final RoutingSlip target, final SystemAddress sender,
                         final T payload, final V extensionData) {
    super(target, sender, payload);

    this.extensionData = extensionData;
  }

  public ExtendedMessage(final RoutingSlip target, final SystemAddress sender,
                         final V extensionData) {
    this(target, sender, null, extensionData);
  }
}
