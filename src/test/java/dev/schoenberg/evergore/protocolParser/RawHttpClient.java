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

	private final int port;

	public RawHttpClient(int port) {
		this.port = port;
	}

	public int statusOf(String requestTarget) {
		return silentThrow(() -> {
			try (Socket socket = new Socket("localhost", port)) {
				socket.setSoTimeout(HANG_GUARD_MILLIS);
				send(socket.getOutputStream(), requestTarget);
				return statusCodeOf(readStatusLine(socket));
			}
		});
	}

	private void send(OutputStream out, String requestTarget) throws Exception {
		out.write(("GET " + requestTarget + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n").getBytes(US_ASCII));
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
