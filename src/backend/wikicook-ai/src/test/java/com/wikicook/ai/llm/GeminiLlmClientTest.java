package com.wikicook.ai.llm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.wikicook.ai.AiUnavailableException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class GeminiLlmClientTest {

    private static final String KEY = "test-key-7f3a9c";
    private static final String PATH = "/v1beta/models/gemini-test:generateContent";
    private static final String SCHEMA = "{\"type\":\"OBJECT\",\"properties\":{\"slots\":{\"type\":\"ARRAY\"}}}";
    private static final LlmRequest REQUEST = new LlmRequest("Bạn là trợ lý", "Lập thực đơn", SCHEMA, 0.4);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @RegisterExtension
    static WireMockExtension gemini = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private GeminiLlmClient client;

    @BeforeEach
    void setUp() {
        client = new GeminiLlmClient(gemini.baseUrl() + "/v1beta", KEY, "gemini-test");
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    private static String reply(String text, String finishReason) {
        return """
                {"candidates":[{"content":{"role":"model","parts":[{"text":%s}]},"finishReason":"%s"}],
                 "modelVersion":"gemini-test-001"}
                """.formatted(com.fasterxml.jackson.databind.node.TextNode.valueOf(text).toString(), finishReason);
    }

    @Test
    void sendsJsonModeWithSchemaAndKeyInHeader() {
        gemini.stubFor(post(urlEqualTo(PATH)).willReturn(okJson(reply("{\"slots\":[]}", "STOP"))));

        LlmResult result = client.complete(REQUEST, TIMEOUT, CancellationToken.none());

        assertThat(result.text()).isEqualTo("{\"slots\":[]}");
        assertThat(result.model()).isEqualTo("gemini-test-001");
        gemini.verify(postRequestedFor(urlEqualTo(PATH)) // no ?key= in the URL
                .withHeader("x-goog-api-key", equalTo(KEY))
                .withRequestBody(matchingJsonPath("$.systemInstruction.parts[0].text", equalTo("Bạn là trợ lý")))
                .withRequestBody(matchingJsonPath("$.contents[0].role", equalTo("user")))
                .withRequestBody(matchingJsonPath("$.contents[0].parts[0].text", equalTo("Lập thực đơn")))
                .withRequestBody(matchingJsonPath("$.generationConfig.temperature", equalTo("0.4")))
                .withRequestBody(matchingJsonPath("$.generationConfig.responseMimeType", equalTo("application/json")))
                .withRequestBody(matchingJsonPath("$.generationConfig.responseSchema.type", equalTo("OBJECT")))
                .withRequestBody(matchingJsonPath("$.generationConfig.responseSchema.properties.slots.type",
                        equalTo("ARRAY"))));
    }

    @Test
    void omitsSchemaAndSystemInstructionWhenAbsent() {
        gemini.stubFor(post(urlEqualTo(PATH)).willReturn(okJson(reply("{}", "STOP"))));

        client.complete(new LlmRequest(null, "Xin chào", null, 0.0), TIMEOUT, CancellationToken.none());

        var body = gemini.getAllServeEvents().getFirst().getRequest().getBodyAsString();
        assertThat(body).doesNotContain("responseSchema").doesNotContain("systemInstruction")
                .contains("\"responseMimeType\":\"application/json\"");
    }

    @Test
    void joinsTextParts() {
        gemini.stubFor(post(urlEqualTo(PATH)).willReturn(okJson("""
                {"candidates":[{"content":{"parts":[{"text":"{\\"a\\":"},{"text":"1}"}]},"finishReason":"STOP"}]}
                """)));

        LlmResult result = client.complete(REQUEST, TIMEOUT, CancellationToken.none());

        assertThat(result.text()).isEqualTo("{\"a\":1}");
        assertThat(result.model()).isEqualTo("gemini-test"); // no modelVersion: configured name
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 429, 500, 503})
    void httpErrorsBecomeUnavailableWithoutProviderDetails(int status, CapturedOutput output) {
        gemini.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":{\"message\":\"API key " + KEY + " quota exceeded\"}}")));

        assertThatThrownBy(() -> client.complete(REQUEST, TIMEOUT, CancellationToken.none()))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining(String.valueOf(status))
                .hasMessageNotContaining(KEY)
                .hasMessageNotContaining("quota exceeded");
        assertThat(output).doesNotContain(KEY);
    }

    @Test
    void neverLogsTheKey(CapturedOutput output) {
        gemini.stubFor(post(urlEqualTo(PATH)).willReturn(okJson(reply("{}", "STOP"))));

        client.complete(REQUEST, TIMEOUT, CancellationToken.none());

        assertThat(output).doesNotContain(KEY);
        assertThat(client.toString()).doesNotContain(KEY);
    }

    @Test
    void slowReplyTimesOut() {
        gemini.stubFor(post(urlEqualTo(PATH)).willReturn(okJson(reply("{}", "STOP")).withFixedDelay(3_000)));

        long start = System.nanoTime();
        assertThatThrownBy(() -> client.complete(REQUEST, Duration.ofMillis(300), CancellationToken.none()))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("time");
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(2_000));
    }

    @Test
    void cancelAbortsTheWaitingRequest() {
        gemini.stubFor(post(urlEqualTo(PATH)).willReturn(okJson(reply("{}", "STOP")).withFixedDelay(5_000)));
        CancellationToken token = CancellationToken.none();
        CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS).execute(token::cancel);

        long start = System.nanoTime();
        assertThatThrownBy(() -> client.complete(REQUEST, Duration.ofSeconds(30), token))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("cancel");
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(3_000));
    }

    @Test
    void alreadyCancelledTokenSkipsTheCall() {
        CancellationToken token = CancellationToken.none();
        token.cancel();

        assertThatThrownBy(() -> client.complete(REQUEST, TIMEOUT, token)).isInstanceOf(AiUnavailableException.class);
        assertThat(gemini.getAllServeEvents()).isEmpty();
    }

    @Test
    void unfinishedReplyIsUnavailable() {
        gemini.stubFor(post(urlEqualTo(PATH)).willReturn(okJson(reply("{\"slots\":[", "MAX_TOKENS"))));

        assertThatThrownBy(() -> client.complete(REQUEST, TIMEOUT, CancellationToken.none()))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("MAX_TOKENS");
    }

    @Test
    void blockedPromptIsUnavailable() {
        gemini.stubFor(post(urlEqualTo(PATH)).willReturn(okJson("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}")));

        assertThatThrownBy(() -> client.complete(REQUEST, TIMEOUT, CancellationToken.none()))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void malformedBodyIsUnavailable() {
        gemini.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody("not json")));

        assertThatThrownBy(() -> client.complete(REQUEST, TIMEOUT, CancellationToken.none()))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void invalidSchemaIsRejectedBeforeCalling() {
        LlmRequest bad = new LlmRequest(null, "x", "{not json", 0.4);

        assertThatThrownBy(() -> client.complete(bad, TIMEOUT, CancellationToken.none()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(gemini.getAllServeEvents()).isEmpty();
    }
}
