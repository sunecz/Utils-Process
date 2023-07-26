package sune.api.process;

import java.nio.file.Path;

final class SynchronousReadOnlyProcess extends ReadOnlyProcessBase {
	
	private StringBuilder string;
	
	SynchronousReadOnlyProcess(Path file) {
		super(file);
	}
	
	@Override
	protected final void readLine(String line) {
		string.append(line);
		string.append(LINE_SEPARATOR);
	}
	
	@Override
	protected final String runAndGetResult() throws Exception {
		// Initialize fields before starting the process
		if(string == null) string = new StringBuilder();
		else               string.setLength(0);
		
		loopRead();
		waitFor();
		dispose();
		
		return string.toString();
	}
}