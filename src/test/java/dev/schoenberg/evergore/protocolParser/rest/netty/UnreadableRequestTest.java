package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.List;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnreadableRequestTest {
	private static final int BUDGET = 64;
	private static final RescuedHeader USER_AGENT = new RescuedHeader("User-Agent", "Firefox/128.0");

	@Test
	void carriesHeadersThatTogetherWithTheConnectionHeaderFillTheBudgetExactly() {
		RescuedHeader small = new RescuedHeader("X-B", "a".repeat(11));

		UnreadableRequest tested = new UnreadableRequest(HttpResponseStatus.BAD_REQUEST, new DefaultHttpHeaders(), List.of(USER_AGENT, small), BUDGET);

		assertThat(tested.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		assertThat(tested.headers().get("X-B")).isEqualTo(small.value());
		assertThat(tested.headers().get("Connection")).isEqualTo("close");
	}

	@Test
	void leavesOutTheLargestHeaderWhenTheHeadersExceedTheBudgetByOneByte() {
		RescuedHeader small = new RescuedHeader("X-B", "a".repeat(12));

		UnreadableRequest tested = new UnreadableRequest(HttpResponseStatus.BAD_REQUEST, new DefaultHttpHeaders(), List.of(USER_AGENT, small), BUDGET);

		assertThat(tested.headers().contains("User-Agent")).isFalse();
		assertThat(tested.headers().get("X-B")).isEqualTo(small.value());
	}

	@Test
	void leavesOutTheLargestHeadersFirstWhateverTheirOrder() {
		RescuedHeader large = new RescuedHeader("X-Large", "a".repeat(30));
		RescuedHeader larger = new RescuedHeader("X-Larger", "a".repeat(40));

		UnreadableRequest tested = new UnreadableRequest(HttpResponseStatus.BAD_REQUEST, new DefaultHttpHeaders(), List.of(large, USER_AGENT, larger), BUDGET);

		assertThat(tested.headers().names()).map(String::toLowerCase).containsExactlyInAnyOrder("user-agent", "connection");
	}
}
