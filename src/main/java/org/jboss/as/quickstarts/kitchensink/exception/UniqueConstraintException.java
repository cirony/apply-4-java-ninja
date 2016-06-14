package org.jboss.as.quickstarts.kitchensink.exception;

import java.util.Set;

public class UniqueConstraintException extends Exception{

	private static final long serialVersionUID = 887942488723396367L;
	
	private Set<String> conflictiveParameters;
	
	public UniqueConstraintException(String message, Set<String> conflictiveParameters){
		super(message);
		this.conflictiveParameters = conflictiveParameters;
	}

	public Set<String> getConflictiveParameters(){
		return this.conflictiveParameters;
	}
}
