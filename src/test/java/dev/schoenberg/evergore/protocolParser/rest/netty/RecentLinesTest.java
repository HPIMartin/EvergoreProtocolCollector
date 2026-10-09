package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.List;
import java.util.stream.Stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;

class RecentLinesTest {
	private static final int CAPACITY = 4;

	private final RecentLines tested = new RecentLines(CAPACITY);

	@Test
	void keepsTheLineBeforeTheCurrentOneAndTheCurrentOneSoFar() {
		append("ab\n");

		append("c");

		assertThat(tested.previousLine()).isEqualTo(bytes("ab\n"));
		assertThat(tested.currentLine()).isEqualTo(bytes("c"));
	}

	@Test
	void joinsTheSlicesOfOneLine() {
		append("a");

		append("b\n");

		assertThat(tested.currentLine()).isEqualTo(bytes("ab\n"));
		assertThat(tested.previousLine()).isEmpty();
	}

	@Test
	void keepsALineThatFillsItsCapacityExactly() {
		append("abc\n");

		assertThat(tested.currentLine()).isEqualTo(bytes("abc\n"));
	}

	@Test
	void keepsNothingOfALineLongerThanItsCapacityNotEvenAfterItEnds() {
		append("abcde");
		assertThat(tested.currentLine()).isEmpty();

		append("f\n");
		assertThat(tested.currentLine()).isEmpty();

		append("g");
		assertThat(tested.previousLine()).isEmpty();
		assertThat(tested.currentLine()).isEqualTo(bytes("g"));
	}

	@Test
	void tellsAMidLineSliceFromALineEndAndFromTheBlankLine() {
		List<SliceEnd> ends = Stream.of("ab", "\n", "b\n", "\r\n", "\n", "\r\r\n").map(this::append).toList();

		assertThat(ends).containsExactly(SliceEnd.MID_LINE, SliceEnd.END_OF_LINE, SliceEnd.END_OF_LINE, SliceEnd.END_OF_HEAD, SliceEnd.END_OF_HEAD, SliceEnd.END_OF_LINE);
	}

	private SliceEnd append(String slice) {
		ByteBuf buffer = Unpooled.copiedBuffer(slice, ISO_8859_1);
		SliceEnd end = tested.append(buffer);
		buffer.release();
		return end;
	}

	private static byte[] bytes(String text) {
		return text.getBytes(ISO_8859_1);
	}
}
