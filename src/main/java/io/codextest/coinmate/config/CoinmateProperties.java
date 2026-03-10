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

    /**
     * Interval at which ping frames are sent on the WebSocket to keep the connection alive
     * across NATs and to detect broken links after system sleep.
     */
    private Duration pingInterval = Duration.ofSeconds(20);

    /**
     * If no messages (including pings/pongs) are observed within this period, the client
     * will time out the receive stream to trigger a reconnect.
     */
    private Duration inactivityTimeout = Duration.ofSeconds(45);

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

    public Duration getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(Duration pingInterval) {
        this.pingInterval = pingInterval;
    }

    public Duration getInactivityTimeout() {
        return inactivityTimeout;
    }

    public void setInactivityTimeout(Duration inactivityTimeout) {
        this.inactivityTimeout = inactivityTimeout;
    }
}
