package dev.schoenberg.evergore.protocolParser;

import dev.schoenberg.evergore.protocolParser.dataExtraction.*;
import io.micronaut.context.event.*;
import io.micronaut.runtime.*;
import io.micronaut.runtime.server.event.*;

public class Application implements ApplicationEventListener<ServerStartupEvent> {
	private final EvergoreDataCollectorJob collector;

	public Application(EvergoreDataCollectorJob collector) {
		this.collector = collector;
	}

	public static void main(String[] args) {
		Micronaut.run(Application.class, args);
	}

	@Override
	public void onApplicationEvent(ServerStartupEvent event) {

	}
}