package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.time.Duration;
import java.util.Map;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ServiceRequests {
	private static final String HOST = "localhost";
	private static final String HEAD_END = "\r\n\r\n";
	private static final Duration HANG_GUARD = Duration.ofSeconds(30);

	private final RunningService service;

	public ServiceRequests(RunningService service) {
		this.service = service;
	}

	public Answer get(String address, Map<String, String> headers) {
		try (Socket socket = new Socket(HOST, service.port())) {
			socket.setSoTimeout((int) HANG_GUARD.toMillis());
			send(socket.getOutputStream(), address, headers);
			return answerIn(socket.getInputStream());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void send(OutputStream out, String address, Map<String, String> headers) throws IOException {
		StringBuilder request = new StringBuilder("GET ").append(address).append(" HTTP/1.0\r\nHost: ").append(HOST).append("\r\nConnection: close\r\n");
		headers.forEach((name, value) -> request.append(name).append(": ").append(value).append("\r\n"));
		out.write(request.append("\r\n").toString().getBytes(UTF_8));
		out.flush();
	}

	private static Answer answerIn(InputStream in) throws IOException {
		String whole = new String(in.readAllBytes(), UTF_8);
		int headEnd = whole.indexOf(HEAD_END);
		String head = headEnd < 0 ? whole : whole.substring(0, headEnd);
		String body = headEnd < 0 ? "" : whole.substring(headEnd + HEAD_END.length());
		String[] statusLine = head.split(" ", 3);
		if (!statusLine[0].startsWith("HTTP/") || statusLine.length < 2) {
			throw new AssertionError("The service answered without a status line: " + head);
		}
		return new Answer(Integer.parseInt(statusLine[1]), body);
	}
}
