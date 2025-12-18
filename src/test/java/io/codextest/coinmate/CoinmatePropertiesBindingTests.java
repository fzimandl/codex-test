package io.codextest.coinmate;

import io.codextest.coinmate.config.CoinmateProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CoinmatePropertiesBindingTests {

    @Autowired
    private CoinmateProperties properties;

    @Test
    void bindsDurationsFromTestProfile() {
        assertThat(properties.getReconnectDelay()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getPingInterval()).isEqualTo(Duration.ofSeconds(15));
        assertThat(properties.getInactivityTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getRestBaseUrl()).isEqualTo("http://localhost");
        assertThat(properties.getWebsocketBaseUrl()).isEqualTo("ws://localhost");
    }
}
