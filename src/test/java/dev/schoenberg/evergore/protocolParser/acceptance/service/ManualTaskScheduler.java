package dev.schoenberg.evergore.protocolParser.acceptance.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.scheduling.ScheduledExecutorTaskScheduler;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;

@Singleton
@Named(TaskExecutors.SCHEDULED)
@Replaces(ScheduledExecutorTaskScheduler.class)
@Requires(env = AcceptanceEnvironment.NAME)
public class ManualTaskScheduler implements TaskScheduler {
	private final List<Runnable> repeatingJobs = new CopyOnWriteArrayList<>();

	public int runRepeatingJobs() {
		repeatingJobs.forEach(Runnable::run);
		return repeatingJobs.size();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Duration initialDelay, Duration delay, Runnable command) {
		repeatingJobs.add(command);
		return new NeverDue();
	}

	@Override
	public ScheduledFuture<?> schedule(String cron, Runnable command) {
		throw unsupported();
	}

	@Override
	public <V> ScheduledFuture<V> schedule(String cron, Callable<V> command) {
		throw unsupported();
	}

	@Override
	public ScheduledFuture<?> schedule(Duration delay, Runnable command) {
		throw unsupported();
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Duration delay, Callable<V> callable) {
		throw unsupported();
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Duration initialDelay, Duration period, Runnable command) {
		throw unsupported();
	}

	private static UnsupportedOperationException unsupported() {
		return new UnsupportedOperationException("The acceptance scenarios fire only fixed-delay jobs, by hand");
	}

	private static class NeverDue implements ScheduledFuture<Object> {
		private volatile boolean cancelled;

		@Override
		public long getDelay(TimeUnit unit) {
			return Long.MAX_VALUE;
		}

		@Override
		public int compareTo(Delayed other) {
			return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			cancelled = true;
			return true;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean isDone() {
			return cancelled;
		}

		@Override
		public Object get() {
			throw new UnsupportedOperationException("A job fired by hand has no result");
		}

		@Override
		public Object get(long timeout, TimeUnit unit) {
			throw new UnsupportedOperationException("A job fired by hand has no result");
		}
	}
}
