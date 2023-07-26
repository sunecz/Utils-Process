package sune.api.process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

abstract class ReadOnlyProcessBase implements ReadOnlyProcess {
	
	protected static final Charset CHARSET        = StandardCharsets.UTF_8;
	protected static final String  LINE_SEPARATOR = "\n"; // Always use Unix line separator
	protected static final int     RESULT_NONE    = Integer.MIN_VALUE;
	
	// Properties
	protected final Path file;
	
	// Process
	private final AtomicInteger result = new AtomicInteger(RESULT_NONE);
	private volatile Process process;
	private volatile BufferedReader reader;
	
	// Flags
	private final AtomicBoolean initializing = new AtomicBoolean();
	private final AtomicBoolean running      = new AtomicBoolean();
	private final AtomicBoolean done         = new AtomicBoolean();
	
	protected ReadOnlyProcessBase(Path file) {
		if(file == null)
			throw new IllegalArgumentException("File cannot be null");
		this.file = file.toAbsolutePath();
		ProcessUtils.registerShutdownHook(() -> {
			try {
				dispose();
			} catch(Exception ex) {
				// Ignore, since shutting down
			}
		});
	}
	
	private final boolean canLoop() {
		return running.get() || (process != null && process.isAlive());
	}
	
	protected final void init(Path dir, String command) throws Exception {
		if(!initializing.compareAndSet(false, true))
			return;
		
		// Reset the result
		result.set(RESULT_NONE);
		
		// Reset the flags
		running.set(false);
		done   .set(false);
		
		// Prepare the given commands
		List<String> commands = new LinkedList<>();
		commands.add(file.toString());
		ProcessUtils.extractCommands(commands, command);
		
		// Create a new process
		ProcessBuilder builder = new ProcessBuilder(commands)
			.directory((dir != null
							? dir
							: file.getParent())
			           	.toFile())
			.redirectErrorStream(true);
		
		// Immediately start the process
		process = builder.start();
		
		// Prepare the reader ahead
		reader = new BufferedReader(new InputStreamReader(process.getInputStream(), CHARSET));
		
		// Reset the flag
		initializing.set(false);
	}
	
	protected final void markAsRunning() {
		running.set(true);
		done   .set(false);
	}
	
	protected final void markAsDone() {
		running.set(false);
		done   .set(true);
	}
	
	protected abstract String runAndGetResult() throws Exception;
	protected abstract void readLine(String line);
	
	protected final void loopRead() throws Exception {
		markAsRunning();
		
		for(String line = null; canLoop();) {
			// Read the next line, this may throw an exception or read null
			line = reader != null ? reader.readLine() : null;
			
			// Check whether the EOF has been reached, if so check whether the process
			// is still active and if it is not, just terminate the loop.
			if(line == null && (process == null || !process.isAlive()))
				break;
			
			// Accept only non-null lines
			if(line != null) {
				readLine(line);
			}
		}
		
		markAsDone();
	}
	
	protected final String execute0(Path dir, String command) throws Exception {
		if(initializing.get() || running.get())
			return null;
		
		init(dir, command);
		return runAndGetResult();
	}
	
	protected void dispose() throws Exception {
		running.set(false);
		
		// Dispose the process
		if(process != null) {
			process.destroyForcibly();
			process = null;
		}
		
		// Dispose the reader
		if(reader != null) {
			reader.close();
			reader = null;
		}
	}
	
	@Override
	public String execute(String command) throws Exception {
		return execute0(null, command);
	}
	
	@Override
	public String execute(String command, Path dir) throws Exception {
		return execute0(dir, command);
	}
	
	@Override
	public int waitFor() throws Exception {
		int res = result.get();
		if(res != RESULT_NONE)
			return res;
		
		if(process != null) {
			res = process.waitFor();
			result.set(res);
		}
		
		markAsDone();
		dispose();
		
		return res;
	}
	
	@Override
	public void close() throws Exception {
		dispose();
	}
	
	@Override
	public Process process() {
		return process;
	}
	
	@Override
	public boolean isRunning() {
		return running.get();
	}
	
	@Override
	public boolean isDone() {
		return done.get();
	}
}