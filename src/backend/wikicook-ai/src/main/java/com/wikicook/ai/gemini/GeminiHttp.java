package com.wikicook.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.llm.CancellationToken;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * POSTs JSON to the Gemini REST API with a per-call timeout and cancellation, shared by the LLM and embedding
 * clients. Each call runs on its own virtual thread so a cancel interrupts it and aborts the HTTP exchange.
 */
public final class GeminiHttp implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(GeminiHttp.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** Extra wait on top of the read timeout before the caller gives up on a stuck worker. */
    private static final Duration GRACE = Duration.ofSeconds(1);

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public GeminiHttp(String baseUrl, String apiKey) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Gemini needs an API key");
        }
        this.apiKey = apiKey;
        // HTTP/1.1: a few calls per generation gain nothing from HTTP/2, and its h2c upgrade breaks on plain-HTTP stubs.
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * @param path URI template with one {@code {model}} variable, e.g. {@code /models/{model}:generateContent}
     * @throws AiUnavailableException on timeout, cancel, HTTP error or a body that is not JSON
     */
    public JsonNode post(String path, String model, Object body, Duration timeout, CancellationToken token) {
        if (token.isCancelled()) {
            throw new AiUnavailableException("AI call cancelled");
        }
        Future<JsonNode> call = executor.submit(() -> send(path, model, body, timeout));
        token.onCancel(() -> call.cancel(true));
        long start = System.nanoTime();
        try {
            return call.get(timeout.plus(GRACE).toMillis(), TimeUnit.MILLISECONDS);
        } catch (CancellationException e) {
            throw new AiUnavailableException("AI call cancelled");
        } catch (TimeoutException e) {
            call.cancel(true);
            throw new AiUnavailableException("Gemini did not answer in time");
        } catch (InterruptedException e) {
            call.cancel(true);
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("AI call interrupted");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof AiUnavailableException unavailable) {
                throw unavailable;
            }
            throw new AiUnavailableException("Gemini call failed", e.getCause());
        } finally {
            log.debug("Gemini {} for model {} took {} ms", path, model,
                    Duration.ofNanos(System.nanoTime() - start).toMillis());
        }
    }

    private JsonNode send(String path, String model, Object body, Duration timeout) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        RestClient rest = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        try {
            JsonNode reply = rest.post()
                    .uri(path, model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (reply == null) {
                throw new AiUnavailableException("Gemini returned an empty body");
            }
            return reply;
        } catch (RestClientResponseException e) {
            // Status only: the provider's error body may echo the request or key details.
            throw new AiUnavailableException("Gemini returned HTTP " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new AiUnavailableException(isTimeout(e) ? "Gemini did not answer in time" : "Gemini unreachable", e);
        } catch (RestClientException e) {
            throw new AiUnavailableException("Gemini returned an unreadable body");
        }
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getClass().getSimpleName().contains("Timeout")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        executor.shutdownNow();
        httpClient.close();
    }
}
