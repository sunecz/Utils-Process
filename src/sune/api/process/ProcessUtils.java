package sune.api.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class ProcessUtils {
	
	private static final List<Runnable> shutdownRunnables = new ArrayList<>();
	private static boolean shutdownInitialized;
	
	// Forbid anyone to create an instance of this class
	private ProcessUtils() {
	}
	
	private static final void shutdownSequence() {
		if(!shutdownRunnables.isEmpty()) {
			shutdownRunnables.forEach(Runnable::run);
		}
	}
	
	public static final void registerShutdownHook(Runnable runnable) {
		if(runnable == null)
			throw new IllegalArgumentException("Runnable cannot be null");
		shutdownRunnables.add(runnable);
		if(!shutdownInitialized) {
			Runtime.getRuntime().addShutdownHook(new Thread(ProcessUtils::shutdownSequence));
			shutdownInitialized = true;
		}
	}
	
	public static final void extractCommands(Collection<String> collection, String command) {
		StringBuilder sb = new StringBuilder();
		
		boolean indq = false;
		boolean insq = false;
		boolean escp = false;
		for(int i = 0, l = command.length(), c, n; i < l; i += n) {
			c = command.codePointAt(i);
			n = Character.charCount(c);
			
			// Escaping
			if(!escp && c == '\\') {
				escp = true;
				continue;
			} else if(escp) {
				escp = false;
				
				if(c != '\"' && c != '\'') {
					sb.appendCodePoint('\\');
				}
				
				sb.appendCodePoint(c);
				continue;
			}
			
			// Inside quotes
			if(indq || insq) {
				if(indq && c == '\"') indq = false; else
				if(insq && c == '\'') insq = false;
				else sb.appendCodePoint(c);
			}
			// Outside of quotes
			else {
				if(c == ' ') {
					// Only add non-empty strings
					if(sb.length() > 0) {
	    				collection.add(sb.toString());
	    				sb.setLength(0);
					}
				} else {
					if(!insq && c == '\"') indq = true; else
					if(!indq && c == '\'') insq = true;
					else sb.appendCodePoint(c);
				}
			}
		}
		
		if(sb.length() > 0) {
			// Add the last left-over string
			collection.add(sb.toString());
		}
	}
}