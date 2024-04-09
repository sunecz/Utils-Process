package sune.api.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

abstract class ReadOnlyProcessBase implements ReadOnlyProcess {
	
	protected static final Charset CHARSET     = StandardCharsets.UTF_8;
	protected static final int     RESULT_NONE = Integer.MIN_VALUE;
	
	protected static final int STATE_NONE         = 0;
	protected static final int STATE_INITIALIZING = 1 << 0;
	protected static final int STATE_RUNNING      = 1 << 1;
	protected static final int STATE_DONE         = 1 << 2;
	protected static final int STATE_DISPOSED     = 1 << 3;
	
	protected final Path file;
	protected final InternalState state = new InternalState(STATE_NONE);
	
	protected volatile int result = RESULT_NONE;
	protected volatile Process process;
	protected volatile BufferedReader reader;
	
	protected ReadOnlyProcessBase(Path file) {
		this.file = Objects.requireNonNull(file).toAbsolutePath();
		ProcessManager.register(this);
	}
	
	protected final ProcessBuilder processBuilder(Path dir, String command) {
		List<String> commands = new ArrayList<>();
		commands.add(file.toString());
		ProcessCommand.extract(commands, command);
		
		return new ProcessBuilder(commands)
			.directory((dir != null ? dir : file.getParent()).toFile())
			.redirectErrorStream(true);
	}
	
	protected final boolean init(Path dir, String command) throws Exception {
		if(!state.compareAndSet(0, STATE_INITIALIZING | STATE_RUNNING, STATE_INITIALIZING)) {
			return false;
		}
		
		try {
			synchronized(this) {
				result = RESULT_NONE;
				process = processBuilder(dir, command).start();
				reader = new BufferedReader(new InputStreamReader(process.getInputStream(), CHARSET));
			}
			
			return true;
		} finally {
			state.clear(STATE_NONE);
		}
	}
	
	protected final boolean isProcessAlive() {
		Process p;
		if((p = process) == null) {
			synchronized(this) {
				if((p = process) == null) {
					return false;
				}
			}
		}
		
		return p.isAlive();
	}
	
	protected final String readLine() throws IOException {
		BufferedReader r;
		if((r = reader) == null) {
			synchronized(this) {
				if((r = reader) == null) {
					return null;
				}
			}
		}
		
		return r.readLine();
	}
	
	protected final void loopRead() {
		while(true) {
			String line = null;
			
			if(state.is(STATE_RUNNING) || isProcessAlive()) {
				try {
					line = readLine();
				} catch(IOException ex) {
					break; // Do not continue
				}
			}
			
			if(line == null) {
				break; // EOF or process no longer alive
			}
			
			processLine(line);
		}
	}
	
	protected final String doExecute(Path dir, String command) throws Exception {
		if(!init(dir, command)) {
			return null;
		}
		
		return runAndGetResult();
	}
	
	protected abstract String runAndGetResult() throws Exception;
	protected abstract void processLine(String line);
	
	protected void dispose() throws Exception {
		// Reset the state only if running. If the process is done or disposed, the state
		// will be the same.
		state.compareAndUnset(STATE_RUNNING, STATE_RUNNING, STATE_RUNNING);
		
		Exception exception = null;
		try {
			synchronized(this) {
				Process p;
				if((p = process) != null) {
					try {
						p.destroyForcibly();
					} catch(Exception ex) {
						exception = ex;
					} finally {
						process = null;
					}
				}
				
				BufferedReader r;
				if((r = reader) != null) {
					try {
						r.close();
					} catch(Exception ex) {
						exception = ex;
					} finally {
						reader = null;
					}
				}
			}
		} finally {
			state.set(STATE_DISPOSED);
			
			if(exception != null) {
				throw exception; // Rethrow
			}
		}
	}
	
	@Override
	public String execute(String command) throws Exception {
		return doExecute(null, command);
	}
	
	@Override
	public String execute(String command, Path dir) throws Exception {
		return doExecute(dir, command);
	}
	
	@Override
	public int waitFor() throws Exception {
		int r;
		if((r = result) != RESULT_NONE) {
			synchronized(this) {
				if((r = result) != RESULT_NONE) {
					return r;
				}
			}
		}
		
		Process p;
		if((p = process) != null) {
			synchronized(this) {
				p = process;
			}
			
			if(p != null) {
				r = p.waitFor(); // Wait outside the synchronized block
				result = r;
			}
		}
		
		return r;
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
		return state.is(STATE_RUNNING);
	}
	
	@Override
	public boolean isDone() {
		return state.is(STATE_DONE);
	}
}