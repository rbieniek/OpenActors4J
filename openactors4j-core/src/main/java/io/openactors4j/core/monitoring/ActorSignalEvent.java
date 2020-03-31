package io.openactors4j.core.monitoring;

import java.time.Duration;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
@Value
public class ActorSignalEvent {
  private String actorName;
  private Duration duration;
  private ActorSignalType signal;
}
