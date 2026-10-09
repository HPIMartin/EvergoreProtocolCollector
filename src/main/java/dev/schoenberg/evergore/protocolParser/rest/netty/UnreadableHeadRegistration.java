package dev.schoenberg.evergore.protocolParser.rest.netty;

import jakarta.inject.Singleton;

import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.http.server.netty.NettyServerCustomizer;

@Singleton
public class UnreadableHeadRegistration implements BeanCreatedEventListener<NettyServerCustomizer.Registry> {
	@Override
	public NettyServerCustomizer.Registry onCreated(BeanCreatedEvent<NettyServerCustomizer.Registry> event) {
		event.getBean().register(new UnreadableHeadCustomizer());
		return event.getBean();
	}
}
