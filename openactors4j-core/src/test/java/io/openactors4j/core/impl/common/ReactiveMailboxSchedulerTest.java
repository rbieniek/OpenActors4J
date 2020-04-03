package io.openactors4j.core.impl.common;

import io.openactors4j.core.common.Mailbox;
import io.openactors4j.core.common.SystemAddress;
import io.openactors4j.core.common.UnboundedMailbox;
import io.openactors4j.core.impl.messaging.Message;
import io.openactors4j.core.impl.messaging.RoutingSlip;
import io.openactors4j.core.impl.messaging.SystemAddressImpl;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReactiveMailboxSchedulerTest {
  private ReactiveMailboxScheduler<TestData> scheduler;
  private SystemAddress testAddress;

  @BeforeEach
  public void setupScheduler() {
    scheduler = new ReactiveMailboxScheduler<>(Executors.newCachedThreadPool());

    scheduler.initialize();
  }

  @BeforeEach
  public void setupAdresses() {
    testAddress = SystemAddressImpl.builder()
        .hostname("localhost")
        .systemName("test-system")
        .transportScheme("local")
        .path("/test")
        .build();
  }

  @Test
  public void shouldDeliverOneMessageToOneMailbox() {
    final TestClient client = TestClient.builder().build();
    final Mailbox<Message<TestData>> mailbox = scheduler.registerMailbox(new UnboundedMailbox<>(), client);

    client.setMailbox(mailbox);

    mailbox.startReceiving();

    mailbox.putMessage(new Message<>(new RoutingSlip(testAddress),
        testAddress,
        TestData.builder()
            .number(1)
            .build()));

    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(() -> client.getCounter() == 1);

    Assertions.assertThat(client.getCounter())
        .isEqualTo(1);
    Assertions.assertThat(client.getPayloads()).containsExactly(TestData.builder()
        .number(1)
        .build());
  }

  @Test
  public void shouldNotDeliverSecondMessageToOneDeregisteredMailbox() {
    final TestClient client = TestClient.builder().build();
    final Mailbox<Message<TestData>> mailbox = scheduler.registerMailbox(new UnboundedMailbox<>(), client);

    client.setMailbox(mailbox);

    mailbox.startReceiving();

    mailbox.putMessage(new Message<>(new RoutingSlip(testAddress),
        testAddress,
        TestData.builder()
            .number(1)
            .build()));

    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(() -> client.getCounter() == 1);

    Assertions.assertThat(client.getCounter())
        .isEqualTo(1);
    Assertions.assertThat(client.getPayloads()).containsExactly(TestData.builder()
        .number(1)
        .build());

    scheduler.deregisterMailbox(mailbox);

    mailbox.putMessage(new Message<>(new RoutingSlip(testAddress),
        testAddress,
        TestData.builder()
            .number(2)
            .build()));

    Assertions.assertThat(client.getCounter())
        .isEqualTo(1);
    Assertions.assertThat(client.getPayloads()).containsExactly(TestData.builder()
        .number(1)
        .build());
  }


  @Test
  public void shouldNotDeliverSecondMessageToOneStoppedAndDeregisteredMailbox() {
    final TestClient client = TestClient.builder().build();
    final Mailbox<Message<TestData>> mailbox = scheduler.registerMailbox(new UnboundedMailbox<>(), client);

    client.setMailbox(mailbox);

    mailbox.startReceiving();

    mailbox.putMessage(new Message<>(new RoutingSlip(testAddress),
        testAddress,
        TestData.builder()
            .number(1)
            .build()));

    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(() -> client.getCounter() == 1);

    Assertions.assertThat(client.getCounter())
        .isEqualTo(1);
    Assertions.assertThat(client.getPayloads()).containsExactly(TestData.builder()
        .number(1)
        .build());

    mailbox.stopReceiving();

    mailbox.putMessage(new Message<>(new RoutingSlip(testAddress),
        testAddress,
        TestData.builder()
            .number(2)
            .build()));

    Assertions.assertThat(client.getCounter())
        .isEqualTo(1);
    Assertions.assertThat(client.getPayloads()).containsExactly(TestData.builder()
        .number(1)
        .build());

    scheduler.deregisterMailbox(mailbox);

    mailbox.putMessage(new Message<>(new RoutingSlip(testAddress),
        testAddress,
        TestData.builder()
            .number(2)
            .build()));

    Assertions.assertThat(client.getCounter())
        .isEqualTo(1);
    Assertions.assertThat(client.getPayloads()).containsExactly(TestData.builder()
        .number(1)
        .build());
  }

  @Test
  public void shouldDeliverManyMessageToOneMailbox() {
    final TestClient client = TestClient.builder().build();
    final Mailbox<Message<TestData>> mailbox = scheduler.registerMailbox(new UnboundedMailbox<>(), client);
    final List<TestData> payloads = new LinkedList<>();

    client.setMailbox(mailbox);

    mailbox.startReceiving();

    for (int counter = 0; counter < 32; counter++) {
      final TestData payload =
          TestData.builder()
              .number(counter)
              .build();

      payloads.add(payload);
      mailbox.putMessage(new Message<>(new RoutingSlip(testAddress),
          testAddress, payload));
    }
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(() -> client.getCounter() == 32);

    Assertions.assertThat(client.getCounter())
        .isEqualTo(32);
    Assertions.assertThat(client.getPayloads()).containsAll(payloads);
  }

  @Test
  public void shouldDeliverMassMessageToOneMailbox() {
    final TestClient client = TestClient.builder().build();
    final Mailbox<Message<TestData>> mailbox = scheduler.registerMailbox(new UnboundedMailbox<>(), client);
    final List<TestData> payloads = new LinkedList<>();

    client.setMailbox(mailbox);

    mailbox.startReceiving();

    for (int counter = 0; counter < 1024; counter++) {
      final TestData payload =
          TestData.builder()
              .number(counter)
              .build();

      payloads.add(payload);
      mailbox.putMessage(new Message<>(new RoutingSlip(testAddress),
          testAddress, payload));
    }
    Awaitility.await()
        .atMost(2, TimeUnit.SECONDS)
        .until(() -> client.getCounter() == 1024);

    Assertions.assertThat(client.getCounter())
        .isEqualTo(1024);
    Assertions.assertThat(client.getPayloads()).containsAll(payloads);
  }

  @Test
  public void shouldDeliverOneMessageToManyMailbox() {
    final List<ImmutablePair<TestClient, Mailbox<Message<TestData>>>> clients = new LinkedList<>();

    for (int counter = 0; counter < 32; counter++) {
      final TestClient client = TestClient.builder().build();
      final Mailbox<Message<TestData>> mailbox = scheduler.registerMailbox(new UnboundedMailbox<>(), client);

      client.setMailbox(mailbox);

      mailbox.startReceiving();

      clients.add(ImmutablePair.of(client, mailbox));
    }

    clients.forEach(pair -> {
      pair.getRight().putMessage(new Message<>(new RoutingSlip(testAddress),
          testAddress,
          TestData.builder()
              .number(1)
              .build()));
    });

    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(() -> clients.stream().map(pair -> pair
            .getLeft()
            .getCounter())
            .reduce((a, b) -> a + b)
            .orElse(0) == 32);

    clients.forEach(pair -> {
      Assertions.assertThat(pair.getLeft().getCounter())
          .isEqualTo(1);
      Assertions.assertThat(pair.getLeft().getPayloads()).containsExactly(TestData.builder()
          .number(1)
          .build());
    });
  }

  @Test
  public void shouldDeliverManyMessageToManyMailbox() {
    final List<ImmutablePair<TestClient, Mailbox<Message<TestData>>>> clients = new LinkedList<>();

    for (int counter = 0; counter < 32; counter++) {
      final TestClient client = TestClient.builder().build();
      final Mailbox<Message<TestData>> mailbox = scheduler.registerMailbox(new UnboundedMailbox<>(), client);

      client.setMailbox(mailbox);

      mailbox.startReceiving();

      clients.add(ImmutablePair.of(client, mailbox));
    }

    for (int counter = 0; counter < 32; counter++) {
      final int value = counter;

      clients.forEach(pair -> {
        pair.getRight().putMessage(new Message<>(new RoutingSlip(testAddress),
            testAddress,
            TestData.builder()
                .number(value)
                .build()));
      });
    }

    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(() -> clients.stream().map(pair -> pair
            .getLeft()
            .getCounter())
            .reduce((a, b) -> a + b)
            .orElse(0) == 1024);

    clients.forEach(pair -> {
      Assertions.assertThat(pair.getLeft().getCounter())
          .isEqualTo(32);
    });
  }

  @Test
  public void shouldDeliverMassMessageToMassMailbox() {
    final List<ImmutablePair<TestClient, Mailbox<Message<TestData>>>> clients = new LinkedList<>();

    for (int counter = 0; counter < 1024; counter++) {
      final TestClient client = TestClient.builder().build();
      final Mailbox<Message<TestData>> mailbox = scheduler.registerMailbox(new UnboundedMailbox<>(), client);

      client.setMailbox(mailbox);

      mailbox.startReceiving();

      clients.add(ImmutablePair.of(client, mailbox));
    }

    for (int counter = 0; counter < 1024; counter++) {
      final int value = counter;

      clients.forEach(pair -> {
        pair.getRight().putMessage(new Message<>(new RoutingSlip(testAddress),
            testAddress,
            TestData.builder()
                .number(value)
                .build()));
      });
    }

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> clients.stream().map(pair -> pair
            .getLeft()
            .getCounter())
            .reduce((a, b) -> a + b)
            .orElse(0) == 1024 * 1024);

    clients.forEach(pair -> {
      Assertions.assertThat(pair.getLeft().getCounter())
          .isEqualTo(1024);
    });
  }

  @Test
  public void shouldDeliverMassMessageToMassMailboxRandom() {
    final List<ImmutablePair<TestClient, Mailbox<Message<TestData>>>> clients = new LinkedList<>();

    for (int counter = 0; counter < 1024; counter++) {
      final TestClient client = TestClient.builder().build();
      final Mailbox<Message<TestData>> mailbox = scheduler.registerMailbox(new UnboundedMailbox<>(), client);

      client.setMailbox(mailbox);

      mailbox.startReceiving();

      clients.add(ImmutablePair.of(client, mailbox));
    }

    final Random random = new Random();

    for (int counter = 0; counter < 1024; counter++) {
      final int value = counter;

      for (int innerCounter = 0; innerCounter < 1024; innerCounter++) {
        int offset = Math.abs(random.nextInt() % 32);

        clients.get(offset).getRight().putMessage(new Message<>(new RoutingSlip(testAddress),
            testAddress,
            TestData.builder()
                .number(value)
                .build()));
      }
    }

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> clients.stream().map(pair -> pair
            .getLeft()
            .getCounter())
            .reduce((a, b) -> a + b)
            .orElse(0) == 1024 * 1024);
  }

  @Test
  public void shouldDeliverOneMessageToOneMailboxWithFlowControl() {
    final TestClient client = TestClient.builder().build();
    final FlowControlledMailbox<Message<TestData>> mailbox = scheduler.registerMailbox(new UnboundedMailbox<>(), client);

    client.setMailbox(mailbox);

    mailbox.startReceiving();
    mailbox.processMode(FlowControlledMailbox.ProcessMode.QUEUE);

    mailbox.putMessage(new Message<>(new RoutingSlip(testAddress),
        testAddress,
        TestData.builder()
            .number(1)
            .build()));

    Assertions.assertThat(client.getCounter())
        .isEqualTo(0);
    Assertions.assertThat(client.getPayloads()).isEmpty();

    mailbox.processMode(FlowControlledMailbox.ProcessMode.DELIVER);

    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(() -> client.getCounter() == 1);

    Assertions.assertThat(client.getCounter())
        .isEqualTo(1);
    Assertions.assertThat(client.getPayloads()).containsExactly(TestData.builder()
        .number(1)
        .build());
  }

  @Test
  public void shouldDeliverManyMessageToManyMailboxWithFlowControl() {
    final List<ImmutablePair<TestClient, FlowControlledMailbox<Message<TestData>>>> clients = new LinkedList<>();

    for (int counter = 0; counter < 32; counter++) {
      final TestClient client = TestClient.builder().build();
      final FlowControlledMailbox<Message<TestData>> mailbox = scheduler.registerMailbox(new UnboundedMailbox<>(), client);

      client.setMailbox(mailbox);

      mailbox.startReceiving();
      mailbox.processMode(FlowControlledMailbox.ProcessMode.QUEUE);

      clients.add(ImmutablePair.of(client, mailbox));
    }

    for (int counter = 0; counter < 32; counter++) {
      final int value = counter;

      clients.forEach(pair -> {
        pair.getRight().putMessage(new Message<>(new RoutingSlip(testAddress),
            testAddress,
            TestData.builder()
                .number(value)
                .build()));
      });
    }

    Assertions.assertThat(clients.stream().map(pair -> pair
        .getLeft()
        .getCounter())
        .reduce((a, b) -> a + b)
        .orElse(0))
        .isEqualTo(0);

    clients.stream().map(pair -> pair.getRight()).forEach(mailbox -> mailbox.processMode(FlowControlledMailbox.ProcessMode.DELIVER));

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> clients.stream().map(pair -> pair
            .getLeft()
            .getCounter())
            .reduce((a, b) -> a + b)
            .orElse(0) == 1024);

    clients.forEach(pair -> {
      Assertions.assertThat(pair.getLeft().getCounter())
          .isEqualTo(32);
    });
  }

  @Builder
  @Data
  private static class TestData {
    private int number;
  }

  @Data
  @Builder
  private static class TestClient implements MailboxSchedulerClient {
    private Mailbox<Message<TestData>> mailbox;
    private int counter = 0;
    private final List<TestData> payloads = new LinkedList<>();

    @Override
    public void takeNextMessage() {
      counter++;
      mailbox.takeMessage().ifPresent(message -> payloads.add(message.getPayload()));
    }
  }
}
