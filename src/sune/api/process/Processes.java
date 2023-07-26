package sune.api.process;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class Processes {
	
	// Forbid anyone to create an instance of this class
	private Processes() {
	}
	
	public static final ReadOnlyProcess createSynchronous(Path file) {
		return new SynchronousReadOnlyProcess(file);
	}
	
	public static final ReadOnlyProcess createAsynchronous(Path file) {
		return createAsynchronous(file, null);
	}
	
	public static final ReadOnlyProcess createAsynchronous(Path file, Consumer<String> listener) {
		return new AsynchronousReadOnlyProcess(file, listener);
	}
}