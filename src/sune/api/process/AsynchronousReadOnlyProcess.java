package sune.api.process;

import java.nio.file.Path;
import java.util.function.Consumer;

final class AsynchronousReadOnlyProcess extends ReadOnlyProcessBase {
	
	private final Consumer<String> listener;
	
	private volatile Thread thread;
	private volatile Exception exception;
	
	AsynchronousReadOnlyProcess(Path file, Consumer<String> listener) {
		super(file);
		this.listener = listener;
	}
	
	private final void execute() {
		state.set(STATE_RUNNING);
		
		try {
			loopRead();
			waitFor();
			state.set(STATE_DONE & ~STATE_RUNNING);
		} catch(Exception ex) {
			exception = ex;
		} finally {
			try {
				dispose();
			} catch(Exception ex) {
				exception = ex;
			}
		}
	}
	
	@Override
	protected final void processLine(String line) {
		if(listener != null) {
			listener.accept(line);
		}
	}
	
	@Override
	protected String runAndGetResult() throws Exception {
		exception = null;
		
		synchronized(this) {
			thread = new Thread(this::execute);
			thread.start();
		}
		
		return null;
	}
	
	@Override
	protected void dispose() throws Exception {
		try {
			Thread t;
			if((t = thread) != null) {
				synchronized(this) {
					if((t = thread) != null) {
						t.interrupt();
						thread = null;
					}
				}
			}
		} finally {
			super.dispose();
		}
	}
	
	@Override
	public int waitFor() throws Exception {
		int result = super.waitFor();
		
		Exception ex;
		if((ex = exception) != null) {
			synchronized(this) {
				if((ex = exception) != null) {
					throw ex;
				}
			}
		}
		
		return result;
	}
}