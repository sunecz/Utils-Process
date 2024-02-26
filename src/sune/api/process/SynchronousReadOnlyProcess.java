package sune.api.process;

import java.nio.file.Path;

final class SynchronousReadOnlyProcess extends ReadOnlyProcessBase {
	
	private static final char LINE_SEPARATOR = '\n'; // Always use the Unix line separator
	
	private StringBuilder string;
	
	SynchronousReadOnlyProcess(Path file) {
		super(file);
	}
	
	@Override
	protected final void processLine(String line) {
		string.append(line);
		string.append(LINE_SEPARATOR);
	}
	
	@Override
	protected final String runAndGetResult() throws Exception {
		state.set(STATE_RUNNING);
		
		if(string == null) {
			string = new StringBuilder();
		} else {
			string.setLength(0);
		}
		
		try {
			loopRead();
			waitFor();
			state.set(STATE_DONE & ~STATE_RUNNING);
			return string.toString();
		} finally {
			dispose();
		}
	}
}