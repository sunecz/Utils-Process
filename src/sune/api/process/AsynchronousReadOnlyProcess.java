package sune.api.process;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

final class AsynchronousReadOnlyProcess extends ReadOnlyProcessBase {
	
	private final Consumer<String> listener;
	private final Object mutex = new Object();
	
	private volatile Thread thread;
	private final AtomicReference<Exception> exception = new AtomicReference<>();
	
	AsynchronousReadOnlyProcess(Path file, Consumer<String> listener) {
		super(file);
		this.listener = listener;
	}
	
	private final void execute() {
		try {
			loopRead();
			waitFor();
			dispose();
		} catch(Exception ex) {
			exception.set(ex);
		}
	}
	
	@Override
	protected final void readLine(String line) {
		if(listener != null) {
			listener.accept(line);
		}
	}
	
	@Override
	protected String runAndGetResult() throws Exception {
		exception.set(null);
		
		synchronized(mutex) {
    		thread = new Thread(this::execute);
    		thread.start();
		}
		
		return null;
	}
	
	@Override
	protected void dispose() throws Exception {
		super.dispose();
		
		synchronized(mutex) {
    		if(thread != null) {
    			thread.interrupt();
    			thread = null;
    		}
		}
	}
	
	@Override
	public int waitFor() throws Exception {
		int result = super.waitFor();
		
		Exception ex = exception.get();
		if(ex != null) throw ex;
		
		return result;
	}
}