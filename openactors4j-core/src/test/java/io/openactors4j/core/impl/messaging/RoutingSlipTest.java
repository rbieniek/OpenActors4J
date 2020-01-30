package io.openactors4j.core.impl.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


import org.junit.jupiter.api.Test;

public class RoutingSlipTest {

  @Test
  public void shouldCreateInstanceWithValidAddress() {
    final RoutingSlip routingSlip = new RoutingSlip(SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("test")
        .hostname("localhost")
        .path("/user/foo")
        .build());

    assertThat(routingSlip).isNotNull();
    assertThat(routingSlip.getAddress()).isEqualTo(SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("test")
        .hostname("localhost")
        .path("/user/foo")
        .build());
    assertThat(routingSlip.getHostName()).isEqualTo("localhost");
    assertThat(routingSlip.getPath()).containsSequence("user", "foo");
    assertThat(routingSlip.getSystemName()).isEqualTo("test");
    assertThat(routingSlip.getTransport()).isEqualTo("local");
  }

  @Test
  public void shouldCreatePathPartsWithValidAddress() {
    final RoutingSlip routingSlip = new RoutingSlip(SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("test")
        .hostname("localhost")
        .path("/user/foo")
        .build());

    assertThat(routingSlip).isNotNull();
    assertThat(routingSlip.havePathPart()).isTrue();
    assertThat(routingSlip.nextPathPart()).hasValue("user");
    assertThat(routingSlip.havePathPart()).isTrue();
    assertThat(routingSlip.nextPathPart()).hasValue("foo");
    assertThat(routingSlip.havePathPart()).isFalse();
    assertThat(routingSlip.nextPathPart()).isEmpty();
  }

  @Test
  public void shouldRaiseExceptionOnShortPath() {
    assertThatIllegalArgumentException().isThrownBy(() -> new RoutingSlip(SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("test")
        .hostname("localhost")
        .path("/user")
        .build()));
  }
}
