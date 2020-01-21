package io.openactors4j.core.impl.common;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.jupiter.api.Test;

public class SystemAddressImplTest {

  @Test
  public void shouldHaveOnlySchemeAndHost() {
    assertThat(SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("test")
        .hostname("localhost")
        .build()
        .transport())
        .hasHost("localhost")
        .hasScheme("actors.local.test")
        .hasPath("")
        .hasPort(0)
        .hasNoQuery()
        .hasNoParameters()
        .hasNoFragment()
        .hasNoUserInfo();
  }

  @Test
  public void shouldHavePort() {
    assertThat(SystemAddressImpl.builder()
        .transportScheme("servlet")
        .systemName("test")
        .hostname("localhost")
        .port(8080)
        .build()
        .transport())
        .hasHost("localhost")
        .hasScheme("actors.servlet.test")
        .hasPort(8080)
        .hasPath("")
        .hasNoQuery()
        .hasNoParameters()
        .hasNoFragment()
        .hasNoUserInfo();
  }

  @Test
  public void shouldHavePath() {
    assertThat(SystemAddressImpl.builder()
        .transportScheme("servlet")
        .systemName("test")
        .hostname("localhost")
        .path("/actors")
        .build()
        .transport())
        .hasHost("localhost")
        .hasScheme("actors.servlet.test")
        .hasPath("/actors")
        .hasPort(0)
        .hasNoQuery()
        .hasNoParameters()
        .hasNoFragment()
        .hasNoUserInfo();
  }
}
