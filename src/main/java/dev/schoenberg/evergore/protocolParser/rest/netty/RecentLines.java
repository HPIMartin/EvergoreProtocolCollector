package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.io.ByteArrayOutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

final class RecentLines {
	private static final byte LINE_FEED = '\n';
	private static final byte CARRIAGE_RETURN = '\r';
	private static final byte[] NO_BYTES = new byte[0];

	private final int capacity;
	private final ByteArrayOutputStream current = new ByteArrayOutputStream();
	private byte[] previous = NO_BYTES;
	private long currentLength;
	private boolean currentStartsWithCarriageReturn;
	private boolean currentEnded;

	RecentLines(int capacity) {
		this.capacity = capacity;
	}

	SliceEnd append(ByteBuf slice) {
		if (currentEnded) {
			moveOnToTheNextLine();
		}
		if (currentLength == 0) {
			currentStartsWithCarriageReturn = slice.getByte(slice.readerIndex()) == CARRIAGE_RETURN;
		}

		currentLength += slice.readableBytes();
		keepWithinCapacity(slice);
		currentEnded = slice.getByte(slice.writerIndex() - 1) == LINE_FEED;
		if (!currentEnded) {
			return SliceEnd.MID_LINE;
		}
		return isBlank() ? SliceEnd.END_OF_HEAD : SliceEnd.END_OF_LINE;
	}

	byte[] previousLine() {
		return previous;
	}

	byte[] currentLine() {
		return current.toByteArray();
	}

	private void moveOnToTheNextLine() {
		previous = current.toByteArray();
		current.reset();
		currentLength = 0;
		currentEnded = false;
	}

	private void keepWithinCapacity(ByteBuf slice) {
		if (currentLength > capacity) {
			current.reset();
			return;
		}

		byte[] bytes = ByteBufUtil.getBytes(slice);
		current.write(bytes, 0, bytes.length);
	}

	private boolean isBlank() {
		return currentLength == 1 || (currentLength == 2 && currentStartsWithCarriageReturn);
	}
}
