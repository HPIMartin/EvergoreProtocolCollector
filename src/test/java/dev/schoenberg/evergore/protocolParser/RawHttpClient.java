package dev.schoenberg.evergore.protocolParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class RawHttpClient {
	private static final int HANG_GUARD_MILLIS = 30_000;
	private static final int PAUSE_BETWEEN_PIECES_MILLIS = 40;

	private final int port;

	public RawHttpClient(int port) {
		this.port = port;
	}

	public int statusOf(String requestTarget) {
		return statusOfRequest("GET " + requestTarget + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n");
	}

	public int statusOfRequest(String request) {
		return silentThrow(() -> {
			try (Socket socket = new Socket("localhost", port)) {
				socket.setSoTimeout(HANG_GUARD_MILLIS);
				send(socket.getOutputStream(), request);
				return statusCodeOf(readStatusLine(socket));
			}
		});
	}

	public String answerToHalfClosedRequest(String request) {
		return silentThrow(() -> {
			try (Socket socket = new Socket("localhost", port)) {
				socket.setSoTimeout(HANG_GUARD_MILLIS);
				send(socket.getOutputStream(), request);
				socket.shutdownOutput();
				return new String(socket.getInputStream().readAllBytes(), US_ASCII);
			}
		});
	}

	public String answerUntilTheServerCloses(String request) {
		return silentThrow(() -> {
			try (Socket socket = new Socket("localhost", port)) {
				socket.setSoTimeout(HANG_GUARD_MILLIS);
				send(socket.getOutputStream(), request);
				return new String(socket.getInputStream().readAllBytes(), US_ASCII);
			}
		});
	}

	public int statusOfRequestSentInPieces(String request, int pieceSize) {
		return silentThrow(() -> {
			try (Socket socket = new Socket("localhost", port)) {
				socket.setSoTimeout(HANG_GUARD_MILLIS);
				socket.setTcpNoDelay(true);
				OutputStream out = socket.getOutputStream();
				for (int start = 0; start < request.length(); start += pieceSize) {
					send(out, request.substring(start, Math.min(start + pieceSize, request.length())));
					Thread.sleep(PAUSE_BETWEEN_PIECES_MILLIS);
				}
				return statusCodeOf(readStatusLine(socket));
			}
		});
	}

	private void send(OutputStream out, String request) throws Exception {
		out.write(request.getBytes(US_ASCII));
		out.flush();
	}

	private String readStatusLine(Socket socket) throws Exception {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), US_ASCII))) {
			return reader.readLine();
		}
	}

	private int statusCodeOf(String statusLine) {
		return parseInt(statusLine.split(" ")[1]);
	}
}
