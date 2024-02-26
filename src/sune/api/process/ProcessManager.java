package sune.api.process;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

final class ProcessManager {
	
	private static final ProcessManager instance = new ProcessManager();
	
	private final List<WeakReference<ReadOnlyProcess>> processes = new ArrayList<>();
	private final AtomicBoolean terminating = new AtomicBoolean();
	
	private ProcessManager() {
		Runtime.getRuntime().addShutdownHook(new Thread(this::terminate));
	}
	
	public static final boolean register(ReadOnlyProcess process) {
		return instance.add(process);
	}
	
	private final boolean add(ReadOnlyProcess process) {
		Objects.requireNonNull(process);
		
		if(terminating.get()) {
			return false; // Already terminating or terminated
		}
		
		synchronized(processes) {
			processes.add(new WeakReference<>(process));
		}
		
		return true;
	}
	
	private final void terminate() {
		if(!terminating.compareAndSet(false, true)) {
			return; // Already terminating or terminated
		}
		
		for(WeakReference<ReadOnlyProcess> ref : processes) {
			ReadOnlyProcess process;
			if((process = ref.get()) != null) {
				try {
					process.close();
				} catch(Exception ex) {
					// Ignore, nothing to do
				}
			}
		}
	}
}