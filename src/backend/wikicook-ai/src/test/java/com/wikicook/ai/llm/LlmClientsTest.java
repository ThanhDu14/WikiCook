package com.wikicook.ai.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.ai.AiUnavailableException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LlmClientsTest {

    private static final LlmRequest REQUEST = new LlmRequest("system", "Lập thực đơn 7 ngày", null, 0.4);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    @Test
    void mockReturnsResponderOutput() {
        MockLlmClient client = new MockLlmClient(request -> "{\"echo\":\"" + request.userPrompt() + "\"}");

        LlmResult result = client.complete(REQUEST, TIMEOUT, CancellationToken.none());

        assertThat(result.text()).isEqualTo("{\"echo\":\"Lập thực đơn 7 ngày\"}");
        assertThat(result.model()).isEqualTo(MockLlmClient.MODEL);
    }

    @Test
    void mockDefaultsToEmptyJsonObject() {
        assertThat(new MockLlmClient().complete(REQUEST, TIMEOUT, CancellationToken.none()).text()).isEqualTo("{}");
    }

    @Test
    void mockRefusesCancelledCall() {
        CancellationToken token = CancellationToken.none();
        token.cancel();

        assertThatThrownBy(() -> new MockLlmClient().complete(REQUEST, TIMEOUT, token))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void offlineAlwaysFails() {
        assertThatThrownBy(() -> new OfflineLlmClient().complete(REQUEST, TIMEOUT, CancellationToken.none()))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("offline");
    }

    @Test
    void requestValidatesInput() {
        assertThatThrownBy(() -> new LlmRequest("s", null, null, 0.4)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LlmRequest("s", "u", null, -0.1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LlmRequest("s", "u", null, 2.1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancellationRunsListenersOnce() {
        CancellationToken token = CancellationToken.none();
        AtomicInteger calls = new AtomicInteger();
        token.onCancel(calls::incrementAndGet);

        token.cancel();
        token.cancel();
        token.onCancel(calls::incrementAndGet); // already cancelled: runs immediately

        assertThat(token.isCancelled()).isTrue();
        assertThat(calls).hasValue(2);
    }
}
