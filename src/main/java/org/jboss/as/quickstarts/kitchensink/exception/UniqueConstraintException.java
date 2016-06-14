package org.jboss.as.quickstarts.kitchensink.exception;

import java.util.Set;

public class UniqueConstraintException extends Exception{

	private static final long serialVersionUID = 887942488723396367L;
	
	private Set<String> conflictiveParameters;
	
	/**
	 * Creates a UniqueConstraintException. This kind of exception is meant to be used when 
	 * trying to register a new Member through the web-form and the validation for the fields that 
	 * must be unique in the database fails.
	 * @param message Descriptive error message 
	 * @param conflictiveParameters Name of the parameters that caused the conflict
	 */
	public UniqueConstraintException(String message, Set<String> conflictiveParameters){
		super(message);
		this.conflictiveParameters = conflictiveParameters;
	}

	/**
	 * Obtain the list of names of the parameters that caused the UniqueConstraintException
	 * @return conflictiveParameters Set with the name of the conflictive parameters
	 */
	public Set<String> getConflictiveParameters(){
		return this.conflictiveParameters;
	}
}
