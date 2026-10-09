package dev.schoenberg.evergore.protocolParser.rest.netty;

import jakarta.inject.Singleton;

import io.micronaut.context.BeanProvider;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.http.server.netty.NettyServerCustomizer;
import io.micronaut.http.server.netty.configuration.NettyHttpServerConfiguration;

@Singleton
public class UnreadableHeadRegistration implements BeanCreatedEventListener<NettyServerCustomizer.Registry> {
	private final BeanProvider<NettyHttpServerConfiguration> configuration;

	public UnreadableHeadRegistration(BeanProvider<NettyHttpServerConfiguration> configuration) {
		this.configuration = configuration;
	}

	@Override
	public NettyServerCustomizer.Registry onCreated(BeanCreatedEvent<NettyServerCustomizer.Registry> event) {
		register(event.getBean());
		return event.getBean();
	}

	void register(NettyServerCustomizer.Registry registry) {
		registry.register(new UnreadableHeadCustomizer(() -> configuration.get().getMaxHeaderSize()));
	}
}
