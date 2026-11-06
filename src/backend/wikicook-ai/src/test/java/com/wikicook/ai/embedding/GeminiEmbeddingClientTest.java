package com.wikicook.ai.embedding;

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
import static org.assertj.core.api.Assertions.within;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.wikicook.ai.AiUnavailableException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class GeminiEmbeddingClientTest {

    private static final String KEY = "test-key-embed-42";
    private static final String MODEL = "gemini-embedding-001";
    private static final String SINGLE = "/v1beta/models/" + MODEL + ":embedContent";
    private static final String BATCH = "/v1beta/models/" + MODEL + ":batchEmbedContents";

    @RegisterExtension
    static WireMockExtension gemini = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private GeminiEmbeddingClient client;

    @BeforeEach
    void setUp() {
        client = new GeminiEmbeddingClient(gemini.baseUrl() + "/v1beta", KEY, MODEL, 768, Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    /** {@code size} values all equal to {@code value}: unit length after normalising is 1/sqrt(size) each. */
    private static String values(int size, double value) {
        return Collections.nCopies(size, String.valueOf(value)).stream().collect(Collectors.joining(",", "[", "]"));
    }

    private static String batchReply(int count, int size) {
        return IntStream.range(0, count).mapToObj(i -> "{\"values\":" + values(size, i + 1) + "}")
                .collect(Collectors.joining(",", "{\"embeddings\":[", "]}"));
    }

    @Test
    void embedSendsTaskTypeAndDimensionsAndNormalises() {
        gemini.stubFor(post(urlEqualTo(SINGLE)).willReturn(okJson("{\"embedding\":{\"values\":" + values(768, 3) + "}}")));

        float[] vector = client.embed("Phở bò", EmbeddingTask.RETRIEVAL_QUERY);

        assertThat(vector).hasSize(768);
        assertThat(vector[0]).isCloseTo((float) (1 / Math.sqrt(768)), within(1e-6f));
        gemini.verify(postRequestedFor(urlEqualTo(SINGLE))
                .withHeader("x-goog-api-key", equalTo(KEY))
                .withRequestBody(matchingJsonPath("$.model", equalTo("models/" + MODEL)))
                .withRequestBody(matchingJsonPath("$.content.parts[0].text", equalTo("Phở bò")))
                .withRequestBody(matchingJsonPath("$.taskType", equalTo("RETRIEVAL_QUERY")))
                .withRequestBody(matchingJsonPath("$.outputDimensionality", equalTo("768"))));
        assertThat(client.model()).isEqualTo(MODEL);
        assertThat(client.dimensions()).isEqualTo(768);
    }

    @Test
    void batchGoesInOneRequestAndKeepsOrder() {
        gemini.stubFor(post(urlEqualTo(BATCH)).willReturn(okJson(batchReply(3, 768))));

        List<float[]> vectors = client.embedBatch(List.of("Phở", "Bún chả", "Cơm tấm"), EmbeddingTask.RETRIEVAL_DOCUMENT);

        assertThat(vectors).hasSize(3).allSatisfy(v -> assertThat(v).hasSize(768));
        gemini.verify(1, postRequestedFor(urlEqualTo(BATCH)));
        gemini.verify(postRequestedFor(urlEqualTo(BATCH))
                .withRequestBody(matchingJsonPath("$.requests[1].content.parts[0].text", equalTo("Bún chả")))
                .withRequestBody(matchingJsonPath("$.requests[2].taskType", equalTo("RETRIEVAL_DOCUMENT")))
                .withRequestBody(matchingJsonPath("$.requests[0].model", equalTo("models/" + MODEL)))
                .withRequestBody(matchingJsonPath("$.requests[0].outputDimensionality", equalTo("768"))));
    }

    @Test
    void largeBatchIsSplitIntoChunksOf100() {
        gemini.stubFor(post(urlEqualTo(BATCH)).atPriority(1)
                .withRequestBody(matchingJsonPath("$.requests[99]"))
                .willReturn(okJson(batchReply(100, 768))));
        gemini.stubFor(post(urlEqualTo(BATCH)).atPriority(2).willReturn(okJson(batchReply(50, 768))));
        List<String> texts = IntStream.range(0, 150).mapToObj(i -> "Món " + i).toList();

        assertThat(client.embedBatch(texts, EmbeddingTask.RETRIEVAL_DOCUMENT)).hasSize(150);
        gemini.verify(2, postRequestedFor(urlEqualTo(BATCH)));
    }

    @Test
    void emptyBatchMakesNoCall() {
        assertThat(client.embedBatch(List.of(), EmbeddingTask.RETRIEVAL_DOCUMENT)).isEmpty();
        assertThat(gemini.getAllServeEvents()).isEmpty();
    }

    @Test
    void wrongDimensionsAreUnavailable() {
        gemini.stubFor(post(urlEqualTo(SINGLE)).willReturn(okJson("{\"embedding\":{\"values\":" + values(3072, 1) + "}}")));

        assertThatThrownBy(() -> client.embed("Phở", EmbeddingTask.RETRIEVAL_QUERY))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("3072");
    }

    @Test
    void missingVectorsInBatchAreUnavailable() {
        gemini.stubFor(post(urlEqualTo(BATCH)).willReturn(okJson(batchReply(1, 768))));

        assertThatThrownBy(() -> client.embedBatch(List.of("a", "b"), EmbeddingTask.RETRIEVAL_DOCUMENT))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void zeroVectorIsUnavailable() {
        gemini.stubFor(post(urlEqualTo(SINGLE)).willReturn(okJson("{\"embedding\":{\"values\":" + values(768, 0) + "}}")));

        assertThatThrownBy(() -> client.embed("Phở", EmbeddingTask.RETRIEVAL_QUERY))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void httpErrorIsUnavailable() {
        gemini.stubFor(post(urlEqualTo(SINGLE)).willReturn(aResponse().withStatus(500).withBody("boom")));

        assertThatThrownBy(() -> client.embed("Phở", EmbeddingTask.RETRIEVAL_QUERY))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("500")
                .hasMessageNotContaining(KEY);
    }
}
