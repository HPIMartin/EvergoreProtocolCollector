package dev.schoenberg.evergore.protocolParser.acceptance.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;
import dev.schoenberg.evergore.protocolParser.helper.concurrent.CollectionThreads;

public class TrackedCollections extends ThreadPoolExecutor {
	private final LedgerFaults faults;
	private final List<FutureTask<Void>> given = new CopyOnWriteArrayList<>();

	public TrackedCollections(LedgerFaults faults) {
		super(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new CollectionThreads());
		this.faults = faults;
	}

	@Override
	public void execute(Runnable collection) {
		FutureTask<Void> task = new FutureTask<>(() -> faults.duringCollection(() -> {
			collection.run();
			return null;
		}));
		given.add(task);
		super.execute(task);
	}

	public void awaitFinished() {
		for (FutureTask<Void> task : given) {
			await(task);
		}
		given.clear();
	}

	private static void await(FutureTask<Void> task) {
		try {
			task.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while a collection was running", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("A collection failed", e.getCause());
		}
	}
}
