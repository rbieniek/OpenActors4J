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
  public void shouldCreateInstanceWithValidSystemNameAndPath() {
    final RoutingSlip routingSlip = new RoutingSlip("test-system", "/user/foo");

    assertThat(routingSlip).isNotNull();
    assertThat(routingSlip.getAddress()).isEqualTo(SystemAddressImpl.builder()
        .transportScheme(AddressConstants.TRANSPORT_SCHEME_LOCAL)
        .systemName("test-system")
        .hostname(AddressConstants.CURRENT_HOST)
        .path("/user/foo")
        .build());
    assertThat(routingSlip.getHostName()).isEqualTo(AddressConstants.CURRENT_HOST);
    assertThat(routingSlip.getPath()).containsSequence("user", "foo");
    assertThat(routingSlip.getSystemName()).isEqualTo("test-system");
    assertThat(routingSlip.getTransport()).isEqualTo(AddressConstants.TRANSPORT_SCHEME_LOCAL);
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
    assertThat(routingSlip.isChildPartAvailable()).isTrue();
    assertThat(routingSlip.nextPathPart()).hasValue("user");
    assertThat(routingSlip.isChildPartAvailable()).isTrue();
    assertThat(routingSlip.nextPathPart()).hasValue("foo");
    assertThat(routingSlip.isChildPartAvailable()).isFalse();
    assertThat(routingSlip.nextPathPart()).isEmpty();
  }

  @Test
  public void shouldRaiseExceptionOnShortPath() {
    assertThatIllegalArgumentException().isThrownBy(() -> new RoutingSlip(SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("test")
        .hostname("localhost")
        .path("/")
        .build()));
  }

  @Test
  public void shouldCopyRoutingSlipInstance() {
    final RoutingSlip routingSlip = new RoutingSlip(SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("test")
        .hostname("localhost")
        .path("/user/foo")
        .build());

    assertThat(routingSlip).isNotNull();
    assertThat(routingSlip.isChildPartAvailable()).isTrue();
    assertThat(routingSlip.nextPathPart()).hasValue("user");
    assertThat(routingSlip.isChildPartAvailable()).isTrue();
    assertThat(routingSlip.nextPathPart()).hasValue("foo");
    assertThat(routingSlip.isChildPartAvailable()).isFalse();
    assertThat(routingSlip.nextPathPart()).isEmpty();

    final RoutingSlip copySlip = RoutingSlip.copy(routingSlip);

    assertThat(copySlip).isNotNull();
    assertThat(copySlip.isChildPartAvailable()).isTrue();
    assertThat(copySlip.nextPathPart()).hasValue("user");
    assertThat(copySlip.isChildPartAvailable()).isTrue();
    assertThat(copySlip.nextPathPart()).hasValue("foo");
    assertThat(copySlip.isChildPartAvailable()).isFalse();
    assertThat(copySlip.nextPathPart()).isEmpty();

  }
}
