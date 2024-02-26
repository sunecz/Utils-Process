package sune.api.process;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

final class InternalState {
	
	private final AtomicInteger state;
	
	public InternalState() {
		this(0);
	}
	
	public InternalState(int initialState) {
		state = new AtomicInteger(initialState);
	}
	
	private static final int opSet(int current, int value) {
		return current | value;
	}
	
	private static final int opUnset(int current, int value) {
		return current & (~value);
	}
	
	private final void setValue(IntUnaryOperator op) {
		for(int value = state.get(), expected;
				(value = state.compareAndExchange(expected = value, op.applyAsInt(value))) != expected;
		);
	}
	
	private final boolean compareAndSetValue(int expected, int mask, IntUnaryOperator op) {
		int value = state.get(), check = expected;
		
		do {
			if((value = state.compareAndExchange(check, op.applyAsInt(value))) == check) {
				return true;
			}
			
			check = value;
		} while(((value & mask) ^ expected) == 0);
		
		return false;
	}
	
	public final void clear(int value) {
		state.set(value);
	}
	
	public final void set(int value) {
		setValue((current) -> opSet(current, value));
	}
	
	public final void unset(int value) {
		setValue((current) -> opUnset(current, value));
	}
	
	public final boolean compareAndSet(int expected, int mask, int value) {
		return compareAndSetValue(expected, mask, (current) -> opSet(current, value));
	}
	
	public final boolean compareAndUnset(int expected, int mask, int value) {
		return compareAndSetValue(expected, mask, (current) -> opUnset(current, value));
	}
	
	public final int get() {
		return state.get();
	}
	
	public final boolean is(int value) {
		return (state.get() & value) == value;
	}
	
	public final boolean is(int value, int mask) {
		return ((state.get() & mask) ^ value) == 0;
	}
}