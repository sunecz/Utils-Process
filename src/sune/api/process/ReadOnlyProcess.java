package sune.api.process;

import java.nio.file.Path;

public interface ReadOnlyProcess extends AutoCloseable {
	
	String execute(String command) throws Exception;
	String execute(String command, Path dir) throws Exception;
	int waitFor() throws Exception;
	
	Process process();
	boolean isRunning();
	boolean isDone();
}