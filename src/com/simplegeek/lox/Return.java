package com.simplegeek.lox;

public class Return extends RuntimeException {
	private static final long serialVersionUID = 525713895917389737L;
	
	final Object value;
	
	Return(Object value) {
		super(null, null, false, false);
		this.value = value;
	}
}
