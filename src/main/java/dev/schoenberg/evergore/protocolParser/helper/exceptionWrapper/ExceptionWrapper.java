package dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper;

public class ExceptionWrapper {
	public static void silentThrow(ThrowingRunnable runnable) {
		try {
			runnable.run();
		} catch (Throwable t) {
			throw runtimeException(t);
		}
	}

	public static <T> T silentThrow(ThrowingSupplier<T> supplier) {
		try {
			return supplier.get();
		} catch (Throwable t) {
			throw runtimeException(t);
		}
	}

	private static RuntimeException runtimeException(Throwable t) {
		if (t instanceof RuntimeException) {
			return (RuntimeException) t;
		} else {
			return new RuntimeException(t);
		}
	}
}
