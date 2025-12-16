package io.codextest.coinmate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
@Profile("!test")
public class OrderBookStreamingRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderBookStreamingRunner.class);

    private final CoinmateApiClient apiClient;
    private final OrderBookWebSocketClient webSocketClient;
    private final OrderBookConversionService conversionService;

    public OrderBookStreamingRunner(CoinmateApiClient apiClient,
                                    OrderBookWebSocketClient webSocketClient,
                                    OrderBookConversionService conversionService) {
        this.apiClient = apiClient;
        this.webSocketClient = webSocketClient;
        this.conversionService = conversionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> pairs = apiClient.fetchCurrencyPairs()
                .collectList()
                .block(Duration.ofSeconds(30));

        if (pairs == null || pairs.isEmpty()) {
            log.error("No currency pairs were returned by Coinmate. Aborting WebSocket subscriptions.");
            return;
        }

        log.info("Subscribing to {} order book streams", pairs.size());

        Mono.whenDelayError(pairs.stream()
                        .map(pair -> webSocketClient.streamOrderBook(pair, conversionService::handleSnapshot))
                        .toList())
                .block();
    }
}
