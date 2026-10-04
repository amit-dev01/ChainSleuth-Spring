package com.chainsleuth.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

/**
 * Enterprise REST client configuration for external blockchain explorers and RPC gateways.
 * <p>
 * Leverages Spring 7's modern fluent {@link RestClient} powered by the JDK 25 {@link HttpClient}
 * engine with native HTTP/2 multiplexing, strictly timed connection parameters, and consistent
 * Jackson serialization standards across all off-chain forensic data sources.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1 — Foundational Infrastructure &amp; Core Configuration</li>
 *   <li><b>Platform Component:</b> Off-Chain REST Explorer Integrations</li>
 *   <li><b>Owner:</b> Lead Integration Engineer &amp; Platform Architect</li>
 * </ul>
 */
@Configuration
public class RestClientConfig {

    private final ChainConfigProperties chainConfig;
    private final ObjectMapper sharedObjectMapper;
    private final MappingJackson2HttpMessageConverter jacksonConverter;

    /**
     * Initializes the REST client configuration with strongly-typed chain settings
     * and sets up the shared, hardened Jackson {@link ObjectMapper}.
     *
     * @param chainConfig immutable blockchain network configuration
     */
    public RestClientConfig(ChainConfigProperties chainConfig) {
        this.chainConfig = chainConfig;
        this.sharedObjectMapper = configureObjectMapper();
        this.jacksonConverter = new MappingJackson2HttpMessageConverter(this.sharedObjectMapper);
    }

    /**
     * Client for Basescan API v1/v2 endpoints.
     * Injects API key parameter via default URI variables for transparent quota authentication.
     *
     * @return configured {@link RestClient} for Basescan
     */
    @Bean("basescanRestClient")
    public RestClient basescanRestClient() {
        return RestClient.builder()
                .baseUrl(chainConfig.base().explorerApiUrl())
                .requestFactory(createClientHttpRequestFactory())
                .defaultUriVariables(Map.of("apikey", chainConfig.base().apiKey()))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(jacksonConverter);
                })
                .build();
    }

    /**
     * Client for Etherscan API v1/v2 endpoints.
     * Injects API key parameter via default URI variables for transparent quota authentication.
     *
     * @return configured {@link RestClient} for Etherscan
     */
    @Bean("etherscanRestClient")
    public RestClient etherscanRestClient() {
        return RestClient.builder()
                .baseUrl(chainConfig.ethereum().explorerApiUrl())
                .requestFactory(createClientHttpRequestFactory())
                .defaultUriVariables(Map.of("apikey", chainConfig.ethereum().apiKey()))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(jacksonConverter);
                })
                .build();
    }

    /**
     * Client for Blockscout v2 REST endpoints.
     * Serves as preferred unthrottled explorer for Base network without API key constraints.
     *
     * @return configured {@link RestClient} for Blockscout
     */
    @Bean("blockscoutRestClient")
    public RestClient blockscoutRestClient() {
        return RestClient.builder()
                .baseUrl("https://base.blockscout.com/api/v2")
                .requestFactory(createClientHttpRequestFactory())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(jacksonConverter);
                })
                .build();
    }

    /**
     * Client for Solana JSON-RPC HTTP Gateway interactions.
     *
     * @return configured {@link RestClient} for Solana RPC
     */
    @Bean("solanaRpcRestClient")
    public RestClient solanaRpcRestClient() {
        return RestClient.builder()
                .baseUrl(chainConfig.solana().rpcUrl())
                .requestFactory(createClientHttpRequestFactory())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(jacksonConverter);
                })
                .build();
    }

    /**
     * Client for TronGrid REST API.
     * Automatically attaches the required TRON-PRO-API-KEY authentication header to all outgoing requests.
     *
     * @return configured {@link RestClient} for TronGrid
     */
    @Bean("tronGridRestClient")
    public RestClient tronGridRestClient() {
        return RestClient.builder()
                .baseUrl(chainConfig.tron().tronGridUrl())
                .requestFactory(createClientHttpRequestFactory())
                .defaultHeader("TRON-PRO-API-KEY", chainConfig.tron().apiKey())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(jacksonConverter);
                })
                .build();
    }

    /**
     * Client for Mempool.space Bitcoin explorer and transaction analysis API.
     *
     * @return configured {@link RestClient} for Mempool.space
     */
    @Bean("mempoolRestClient")
    public RestClient mempoolRestClient() {
        return RestClient.builder()
                .baseUrl(chainConfig.bitcoin().mempoolApiUrl())
                .requestFactory(createClientHttpRequestFactory())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(jacksonConverter);
                })
                .build();
    }

    /**
     * Builds a JDK-backed HTTP request factory utilizing Java 25 HttpClient with HTTP/2 transport.
     *
     * @return {@link ClientHttpRequestFactory} backed by JDK HttpClient
     */
    private ClientHttpRequestFactory createClientHttpRequestFactory() {
        return new JdkClientHttpRequestFactory(buildHttpClient());
    }

    /**
     * Instantiates the shared JDK 25 {@link HttpClient} configured for HTTP/2 multiplexing.
     *
     * @return configured {@link HttpClient} instance
     */
    private HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Configures the enterprise Jackson {@link ObjectMapper} with ISO-8601 timestamps
     * and non-failing deserialization of unexpected explorer payload fields.
     *
     * @return configured {@link ObjectMapper}
     */
    private ObjectMapper configureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}
