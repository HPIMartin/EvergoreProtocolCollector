package dev.schoenberg.evergore.protocolParser.helper.security;

import jakarta.inject.Singleton;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.application.AdminKey;

@Singleton
public class AdminKeyAnnouncement implements ApplicationEventListener<StartupEvent> {
	private final AdminKey adminKey;
	private final Logger logger;

	public AdminKeyAnnouncement(AdminKey adminKey, Logger logger) {
		this.adminKey = adminKey;
		this.logger = logger;
	}

	@Override
	public void onApplicationEvent(StartupEvent event) {
		adminKey.announceTo(logger);
	}
}
