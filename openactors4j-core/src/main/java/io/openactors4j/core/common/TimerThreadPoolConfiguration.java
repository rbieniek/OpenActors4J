package io.openactors4j.core.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class TimerThreadPoolConfiguration {
  @Builder.Default
  private int corePoolSize = 10;
}
