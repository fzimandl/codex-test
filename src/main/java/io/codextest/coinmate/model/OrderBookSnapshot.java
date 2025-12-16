package io.codextest.coinmate.model;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderBookSnapshot(String currencyPair,
                                BigDecimal bestBid,
                                BigDecimal bestAsk,
                                Instant receivedAt) {
}
