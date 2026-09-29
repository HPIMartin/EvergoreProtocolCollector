package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class LedgerFaults {
	private final Set<String> unreadableMembers = ConcurrentHashMap.newKeySet();
	private volatile boolean collecting;
	private volatile boolean figuresWithheld;
	private volatile boolean figuresRefused;

	public void makeUnreadable(String member) {
		unreadableMembers.add(member);
	}

	public void makeReadable(String member) {
		unreadableMembers.remove(member);
	}

	public void withholdFigures() {
		figuresWithheld = true;
	}

	public void refuseToSaveFigures() {
		figuresRefused = true;
	}

	public void saveFiguresAgain() {
		figuresRefused = false;
	}

	public <T> T duringCollection(Supplier<T> collection) {
		collecting = true;
		try {
			return collection.get();
		} finally {
			collecting = false;
		}
	}

	public <T> T guardLedger(Class<T> port, T ledger) {
		return guard(port, ledger, arguments -> collecting && namesAnUnreadableMember(arguments));
	}

	public <T> T guardFigures(Class<T> port, T figures) {
		return guard(port, figures, arguments -> collecting ? figuresRefused : figuresWithheld);
	}

	private boolean namesAnUnreadableMember(Object[] arguments) {
		return arguments != null && Arrays.stream(arguments).anyMatch(unreadableMembers::contains);
	}

	private static <T> T guard(Class<T> port, T target, Fault fault) {
		return port.cast(Proxy.newProxyInstance(port.getClassLoader(), new Class<?>[]{port}, (proxy, method, arguments) -> {
			if (fault.strikes(arguments)) {
				throw new IllegalStateException("The acceptance scenario made this read fail: " + method.getName());
			}
			return invoke(target, method, arguments);
		}));
	}

	private static Object invoke(Object target, Method method, Object[] arguments) throws Throwable {
		try {
			return method.invoke(target, arguments);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private interface Fault {
		boolean strikes(Object[] arguments);
	}
}
