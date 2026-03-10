package io.codextest.coinmate.service;

import io.codextest.coinmate.model.ConversionDirection;
import io.codextest.coinmate.model.ExchangeRate;
import io.codextest.coinmate.model.OrderBookSnapshot;
import io.codextest.coinmate.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderBookConversionService {

    private static final Logger log = LoggerFactory.getLogger(OrderBookConversionService.class);
    private static final String BTC_EUR = "BTC_EUR";
    private static final String BTC_CZK = "BTC_CZK";
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private final Map<String, OrderBookSnapshot> latestSnapshots = new ConcurrentHashMap<>();
    private final DecimalFormat decimalFormat;
    private final ExchangeRateRepository exchangeRateRepository;

    public OrderBookConversionService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        this.decimalFormat = new DecimalFormat("#,##0.0000######", symbols);
    }

    public void handleSnapshot(OrderBookSnapshot snapshot) {
        latestSnapshots.put(snapshot.currencyPair(), snapshot);
        if (snapshot.bestBid() != null && snapshot.bestAsk() != null) {
            log.info("order_book [{}] bid={} ask={}", snapshot.currencyPair(),
                    decimalFormat.format(snapshot.bestBid()), decimalFormat.format(snapshot.bestAsk()));
        }
        maybeReportConversionRate();
    }

    private void maybeReportConversionRate() {
        OrderBookSnapshot btcEur = latestSnapshots.get(BTC_EUR);
        OrderBookSnapshot btcCzk = latestSnapshots.get(BTC_CZK);
        if (!isValid(btcEur) || !isValid(btcCzk)) {
            return;
        }

        BigDecimal eurToCzk = btcCzk.bestBid().divide(btcEur.bestAsk(), MC);
        // For CZK -> EUR, user wants to see how many CZK are needed per 1 EUR.
        // That corresponds to buying BTC for CZK at the CZK ask, and selling BTC for EUR at the EUR bid.
        BigDecimal czkPerEur = btcCzk.bestAsk().divide(btcEur.bestBid(), MC);

        log.info("EUR→CZK via BTC: 1 EUR ≈ {} CZK (buy BTC @ {} EUR, sell BTC @ {} CZK)",
                decimalFormat.format(eurToCzk),
                decimalFormat.format(btcEur.bestAsk()),
                decimalFormat.format(btcCzk.bestBid()));

        log.info("CZK→EUR via BTC: 1 EUR ≈ {} CZK (buy BTC @ {} CZK, sell BTC @ {} EUR)",
                decimalFormat.format(czkPerEur),
                decimalFormat.format(btcCzk.bestAsk()),
                decimalFormat.format(btcEur.bestBid()));

        // Persist both rates with timestamp, including picked amounts when available
        try {
            ExchangeRate eurToCzkRate = new ExchangeRate(
                    ConversionDirection.EUR_TO_CZK,
                    eurToCzk,
                    btcCzk.bestBidAmount(), // bid amount from CZK book
                    btcEur.bestAskAmount(), // ask amount from EUR book
                    java.time.Instant.now());
            ExchangeRate czkToEurRate = new ExchangeRate(
                    ConversionDirection.CZK_TO_EUR,
                    czkPerEur,
                    btcEur.bestBidAmount(), // bid amount from EUR book
                    btcCzk.bestAskAmount(), // ask amount from CZK book
                    java.time.Instant.now());
            exchangeRateRepository.save(eurToCzkRate);
            exchangeRateRepository.save(czkToEurRate);
        } catch (Exception e) {
            log.warn("Failed to persist exchange rates: {}", e.getMessage());
        }
    }

    private boolean isValid(OrderBookSnapshot snapshot) {
        return snapshot != null
                && snapshot.bestBid() != null
                && snapshot.bestAsk() != null
                && snapshot.bestBid().compareTo(BigDecimal.ZERO) > 0
                && snapshot.bestAsk().compareTo(BigDecimal.ZERO) > 0;
    }
}
