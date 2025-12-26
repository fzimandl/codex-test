package io.codextest.coinmate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codextest.coinmate.config.CoinmateProperties;
import io.codextest.coinmate.model.OrderBookSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

@Component
public class OrderBookWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(OrderBookWebSocketClient.class);

    private final ReactorNettyWebSocketClient webSocketClient;
    private final ObjectMapper objectMapper;
    private final CoinmateProperties properties;

    public OrderBookWebSocketClient(ObjectMapper objectMapper, CoinmateProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.webSocketClient = new ReactorNettyWebSocketClient();
    }

    public Mono<Void> streamOrderBook(String currencyPair, Consumer<OrderBookSnapshot> consumer) {
        return Mono.defer(() -> {
                    URI uri = URI.create(String.format("%s/api/websocket/channel/order-book/%s",
                            properties.getWebsocketBaseUrl(), currencyPair));
                    log.info("Connecting to order book stream for {}", currencyPair);
                    return webSocketClient.execute(uri, session -> {
                        // Heartbeat: send periodic WebSocket ping frames
                        Flux<WebSocketMessage> pings = Flux.interval(properties.getPingInterval())
                                .map(tick -> session.pingMessage(factory -> factory.wrap(new byte[] {1})))
                                .doOnSubscribe(sub -> log.debug("Starting ping heartbeat for {}", currencyPair))
                                .doOnError(err -> log.debug("Ping stream error for {}: {}", currencyPair, err.toString()));

                        // Inbound processing with inactivity timeout to trigger reconnect after sleep
                        Mono<Void> inbound = session.receive()
                                // Trigger reconnect if nothing is received for the timeout period (e.g., after sleep)
                                .timeout(properties.getInactivityTimeout())
                                .flatMap(msg -> {
                                    switch (msg.getType()) {
                                        case TEXT:
                                            return extractSnapshot(currencyPair, msg.getPayloadAsText())
                                                    .doOnNext(consumer::accept);
                                        case PONG:
                                        case PING:
                                        case BINARY:
                                        default:
                                            return Mono.empty();
                                    }
                                })
                                // If the server closes the connection cleanly, convert completion to an error
                                // so our retryWhen(...) will perform a seamless reconnect.
                                .then(Mono.error(new IllegalStateException("WebSocket closed for " + currencyPair)));

                        return session.send(pings).and(inbound);
                    });
                })
                .retryWhen(Retry.backoff(Long.MAX_VALUE, properties.getReconnectDelay())
                        .doBeforeRetry(retrySignal -> log.warn("Reconnecting {} after error: {}",
                                currencyPair, retrySignal.failure().getMessage()))
                        .maxBackoff(Duration.ofMinutes(1)))
                .doOnCancel(() -> log.info("Stream cancelled for {}", currencyPair));
    }

    private Mono<OrderBookSnapshot> extractSnapshot(String currencyPair, String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String event = root.hasNonNull("event") ? root.get("event").asText() : null;
            if (event != null && !"data".equalsIgnoreCase(event)) {
                return Mono.empty();
            }
            JsonNode payload = root.path("payload");
            if (payload.isMissingNode()) {
                return Mono.empty();
            }
            PickedLevel bestBid = pickPrice(payload.path("bids"), true);
            PickedLevel bestAsk = pickPrice(payload.path("asks"), false);
            if (bestBid == null || bestAsk == null || bestBid.price == null || bestAsk.price == null) {
                return Mono.empty();
            }
            return Mono.just(new OrderBookSnapshot(
                    currencyPair,
                    bestBid.price,
                    bestBid.amount,
                    bestAsk.price,
                    bestAsk.amount,
                    Instant.now()
            ));
        } catch (Exception ex) {
            log.warn("Failed to parse order book payload for {}: {}", currencyPair, ex.getMessage());
            return Mono.empty();
        }
    }

    private PickedLevel pickPrice(JsonNode levels, boolean pickHighest) {
        if (levels == null || !levels.isArray() || levels.isEmpty()) {
            return null;
        }
        BigDecimal bestPrice = null;
        BigDecimal bestAmount = null;
        for (JsonNode level : levels) {
            JsonNode priceNode = level.path("price");
            JsonNode amountNode = level.path("amount");
            if (!priceNode.isNumber()) {
                continue;
            }
            BigDecimal price = priceNode.decimalValue();
            BigDecimal amount = amountNode.isNumber() ? amountNode.decimalValue() : null;
            if (bestPrice == null) {
                bestPrice = price;
                bestAmount = amount;
                continue;
            }
            int comparison = price.compareTo(bestPrice);
            if ((pickHighest && comparison > 0) || (!pickHighest && comparison < 0)) {
                bestPrice = price;
                bestAmount = amount;
            }
        }
        return new PickedLevel(bestPrice, bestAmount);
    }

    private static final class PickedLevel {
        final BigDecimal price;
        final BigDecimal amount;

        private PickedLevel(BigDecimal price, BigDecimal amount) {
            this.price = price;
            this.amount = amount;
        }
    }
}
