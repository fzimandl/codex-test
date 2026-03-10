package io.codextest.coinmate;

import io.codextest.coinmate.service.OrderBookWebSocketClient;
import io.codextest.coinmate.model.OrderBookSnapshot;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderBookWebSocketPongTests {

    private static DisposableServer wsServer;
    private static int port;

    @BeforeAll
    static void startServer() {
        wsServer = HttpServer.create()
                .port(0)
                .route(routes -> routes.ws("/api/websocket/channel/order-book/{pair}", (in, out) -> {
                    // Mixed stream: frequent TEXT frames and some PONG frames
                    String json = "{\"event\":\"data\",\"payload\":{\"bids\":[{\"price\":10001}],\"asks\":[{\"price\":10011}]}}";

                    var textFrames = reactor.core.publisher.Flux.interval(Duration.ofMillis(100))
                            .map(i -> (Object) new TextWebSocketFrame(json));

                    var pongFrames = reactor.core.publisher.Flux.interval(Duration.ofMillis(350))
                            .map(i -> (Object) new PongWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{1})));

                    var mixed = reactor.core.publisher.Flux.merge(textFrames, pongFrames)
                            .take(30); // limit to keep test bounded

                    return out.sendObject(mixed).neverComplete();
                }))
                .bindNow();
        port = wsServer.port();
    }

    @AfterAll
    static void stopServer() {
        if (wsServer != null) {
            wsServer.disposeNow();
        }
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("coinmate.websocket-base-url", () -> "ws://localhost:" + port);
    }

    @Autowired
    private OrderBookWebSocketClient client;

    @Test
    void ignoresPongFramesAndProcessesTextSnapshots() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        List<OrderBookSnapshot> received = new CopyOnWriteArrayList<>();

        Disposable subscription = client
                .streamOrderBook("BTC_EUR", snapshot -> {
                    received.add(snapshot);
                    latch.countDown();
                })
                .onErrorResume(e -> Mono.empty())
                .subscribe();

        try {
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).as("expected to receive 3 snapshots within timeout").isTrue();
            assertThat(received).hasSizeGreaterThanOrEqualTo(3);
            // Basic sanity: values parsed as provided by server
            assertThat(received.get(0).bestBid()).isNotNull();
            assertThat(received.get(0).bestAsk()).isNotNull();
        } finally {
            subscription.dispose();
        }
    }
}
