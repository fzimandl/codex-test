package io.codextest.coinmate;

import io.codextest.coinmate.config.CoinmateProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CoinmateProperties.class)
public class CoinmateStreamerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoinmateStreamerApplication.class, args);
    }
}
