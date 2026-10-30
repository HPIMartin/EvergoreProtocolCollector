package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ServiceRequests {
	private static final String HOST = "localhost";
	private static final String LINE_END = "\r\n";
	private static final String HEAD_END = LINE_END + LINE_END;
	private static final String JSON = "application/json";
	private static final Duration HANG_GUARD = Duration.ofSeconds(30);

	private final RunningService service;

	public ServiceRequests(RunningService service) {
		this.service = service;
	}

	public Answer get(String address, Map<String, String> headers) {
		return exchange("GET", address, headers, new byte[0]);
	}

	public Answer ask(Asking asking, Map<String, String> headers) {
		return asking.isPost() ? post(asking.address(), "", headers) : get(asking.address(), headers);
	}

	public Answer post(String address, String jsonBody, Map<String, String> headers) {
		Map<String, String> withBody = new HashMap<>(headers);
		withBody.put("Content-Type", JSON);
		return exchange("POST", address, withBody, jsonBody.getBytes(UTF_8));
	}

	private Answer exchange(String method, String address, Map<String, String> headers, byte[] body) {
		try (Socket socket = new Socket(HOST, service.port())) {
			socket.setSoTimeout((int) HANG_GUARD.toMillis());
			send(socket.getOutputStream(), method, address, headers, body);
			return answerIn(socket.getInputStream());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void send(OutputStream out, String method, String address, Map<String, String> headers, byte[] body) throws IOException {
		StringBuilder request = new StringBuilder(method).append(' ').append(address).append(" HTTP/1.0").append(LINE_END).append("Host: ").append(HOST).append(LINE_END);
		request.append("Connection: close").append(LINE_END);
		if (body.length > 0) {
			request.append("Content-Length: ").append(body.length).append(LINE_END);
		}
		headers.forEach((name, value) -> request.append(name).append(": ").append(value).append(LINE_END));
		out.write(request.append(LINE_END).toString().getBytes(UTF_8));
		out.write(body);
		out.flush();
	}

	private static Answer answerIn(InputStream in) throws IOException {
		String whole = new String(in.readAllBytes(), UTF_8);
		int headEnd = whole.indexOf(HEAD_END);
		String head = headEnd < 0 ? whole : whole.substring(0, headEnd);
		String body = headEnd < 0 ? "" : whole.substring(headEnd + HEAD_END.length());
		String[] lines = head.split(LINE_END);
		String[] statusLine = lines[0].split(" ", 3);
		if (!statusLine[0].startsWith("HTTP/") || statusLine.length < 2) {
			throw new AssertionError("The service answered without a status line: " + head);
		}
		return new Answer(Integer.parseInt(statusLine[1]), body, headersIn(lines));
	}

	private static Map<String, List<String>> headersIn(String[] lines) {
		Map<String, List<String>> headers = new HashMap<>();
		for (int line = 1; line < lines.length; line++) {
			int separator = lines[line].indexOf(':');
			if (separator > 0) {
				headers.computeIfAbsent(lines[line].substring(0, separator).toLowerCase(Locale.ROOT), name -> new ArrayList<>()).add(lines[line].substring(separator + 1).trim());
			}
		}
		return headers;
	}
}
