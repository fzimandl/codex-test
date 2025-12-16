package io.codextest.coinmate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversionDirection direction;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal rate;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    protected ExchangeRate() {
        // JPA
    }

    public ExchangeRate(ConversionDirection direction, BigDecimal rate, Instant computedAt) {
        this.direction = direction;
        this.rate = rate;
        this.computedAt = computedAt;
    }

    public Long getId() {
        return id;
    }

    public ConversionDirection getDirection() {
        return direction;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public Instant getComputedAt() {
        return computedAt;
    }
}
