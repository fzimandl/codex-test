package io.codextest.coinmate.service;

import io.codextest.coinmate.config.CoinmateProperties;
import io.codextest.coinmate.model.ApiResponse;
import io.codextest.coinmate.model.TradingPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Component
public class CoinmateApiClient {

    private static final Logger log = LoggerFactory.getLogger(CoinmateApiClient.class);

    private final WebClient webClient;
    private final Duration requestTimeout = Duration.ofSeconds(10);

    public CoinmateApiClient(WebClient.Builder builder, CoinmateProperties properties) {
        this.webClient = builder
                .baseUrl(properties.getRestBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Flux<String> fetchCurrencyPairs() {
        return webClient.get()
                .uri("/api/tradingPairs")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<TradingPair>>>() {})
                .timeout(requestTimeout)
                .flatMapMany(this::toPairs)
                .sort(Comparator.naturalOrder())
                .doOnSubscribe(subscription -> log.info("Requesting available currency pairs from Coinmate API"))
                .doOnComplete(() -> log.info("Finished loading currency pairs"));
    }

    private Flux<String> toPairs(ApiResponse<List<TradingPair>> response) {
        if (response == null) {
            return Flux.empty();
        }
        if (response.isError()) {
            return Flux.error(new IllegalStateException("Coinmate API responded with error: " + response.getErrorMessage()));
        }
        List<TradingPair> pairs = response.getData();
        if (pairs == null || pairs.isEmpty()) {
            log.warn("Coinmate API returned an empty trading pair list");
            return Flux.empty();
        }
        return Flux.fromIterable(pairs)
                .map(TradingPair::getName)
                .filter(name -> name != null && !name.isBlank());
    }
}
