package io.openactors4j.core.impl.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


import org.junit.jupiter.api.Test;

public class SystemAddressImplTest {

  @Test
  public void shouldHaveOnlySchemeAndHost() {
    assertThat(SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("test")
        .hostname("localhost")
        .path("/foo")
        .build()
        .transport())
        .hasHost("localhost")
        .hasScheme("actors.local.test")
        .hasPath("/foo")
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
        .path("/foo")
        .build()
        .transport())
        .hasHost("localhost")
        .hasScheme("actors.servlet.test")
        .hasPort(8080)
        .hasPath("/foo")
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

  @Test
  public void shouldRaiseExceptionOnMalformedSchemeMissingTransport() {
    assertThatIllegalArgumentException().isThrownBy(() -> SystemAddressImpl.builder()
        .systemName("test")
        .hostname("localhost")
        .path("/user/foo")
        .build()
        .validate());
  }

  @Test
  public void shouldRaiseExceptionOnMalformedSchemeMissingSystem() {
    assertThatIllegalArgumentException().isThrownBy(() -> SystemAddressImpl.builder()
        .transportScheme("local")
        .hostname("localhost")
        .path("/user/foo")
        .build()
        .validate());
  }

  @Test
  public void shouldRaiseExceptionOnMalformedSchemeBlankTransport() {
    assertThatIllegalArgumentException().isThrownBy(() -> SystemAddressImpl.builder()
        .systemName("test")
        .transportScheme("")
        .hostname("localhost")
        .path("/user/foo")
        .build()
        .validate());
  }

  @Test
  public void shouldRaiseExceptionOnMalformedSchemeBlankSystem() {
    assertThatIllegalArgumentException().isThrownBy(() -> SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("")
        .hostname("localhost")
        .path("/user/foo")
        .build()
        .validate());
  }

  @Test
  public void shouldRaiseExceptionOnEmptyPathPart() {
    assertThatIllegalArgumentException().isThrownBy(() -> SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("test")
        .hostname("localhost")
        .path("/user//foo")
        .build()
        .validate());
  }

  @Test
  public void shouldRaiseExceptionOnMissingHostname() {
    assertThatIllegalArgumentException().isThrownBy(() -> SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("test")
        .path("/user/foo")
        .build()
        .validate());
  }

  @Test
  public void shouldRaiseExceptionOnBlankHostname() {
    assertThatIllegalArgumentException().isThrownBy(() -> SystemAddressImpl.builder()
        .transportScheme("local")
        .systemName("test")
        .path("/user/foo")
        .build()
        .validate());
  }
}
