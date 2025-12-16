package io.codextest.coinmate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "coinmate")
public class CoinmateProperties {

    /**
     * Base REST endpoint that exposes the Coinmate HTTP API.
     */
    private String restBaseUrl = "https://coinmate.io";

    /**
     * Base WebSocket endpoint that streams public events.
     */
    private String websocketBaseUrl = "wss://coinmate.io";

    /**
     * Delay before attempting to reconnect to the WebSocket stream.
     */
    private Duration reconnectDelay = Duration.ofSeconds(5);

    public String getRestBaseUrl() {
        return restBaseUrl;
    }

    public void setRestBaseUrl(String restBaseUrl) {
        this.restBaseUrl = restBaseUrl;
    }

    public String getWebsocketBaseUrl() {
        return websocketBaseUrl;
    }

    public void setWebsocketBaseUrl(String websocketBaseUrl) {
        this.websocketBaseUrl = websocketBaseUrl;
    }

    public Duration getReconnectDelay() {
        return reconnectDelay;
    }

    public void setReconnectDelay(Duration reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }
}
