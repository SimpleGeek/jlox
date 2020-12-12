package com.simplegeek.lox;

public class RuntimeError extends RuntimeException {
	private static final long serialVersionUID = 9163439923176683627L;
	
	final Token token;
	
	RuntimeError(Token token, String message) {
		super(message);
		this.token = token;
	}
}
