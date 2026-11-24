package dev.schoenberg.evergore.protocolParser.helper.concurrent;

import java.util.concurrent.ThreadFactory;

public class CollectionThreads implements ThreadFactory {
	@Override
	public Thread newThread(Runnable task) {
		Thread thread = new Thread(task, "collection");
		thread.setDaemon(true);
		return thread;
	}
}
