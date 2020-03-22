package protocolParser.helper.exceptionWrapper;

public interface ThrowingSupplier<T> {
	T get() throws Throwable;
}
