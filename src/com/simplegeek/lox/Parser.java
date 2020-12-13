package com.simplegeek.lox;

import static com.simplegeek.lox.TokenType.BANG;
import static com.simplegeek.lox.TokenType.BANG_EQUAL;
import static com.simplegeek.lox.TokenType.EOF;
import static com.simplegeek.lox.TokenType.EQUAL_EQUAL;
import static com.simplegeek.lox.TokenType.FALSE;
import static com.simplegeek.lox.TokenType.GREATER;
import static com.simplegeek.lox.TokenType.GREATER_EQUAL;
import static com.simplegeek.lox.TokenType.LEFT_PAREN;
import static com.simplegeek.lox.TokenType.LESS;
import static com.simplegeek.lox.TokenType.LESS_EQUAL;
import static com.simplegeek.lox.TokenType.MINUS;
import static com.simplegeek.lox.TokenType.NIL;
import static com.simplegeek.lox.TokenType.NUMBER;
import static com.simplegeek.lox.TokenType.PLUS;
import static com.simplegeek.lox.TokenType.RIGHT_PAREN;
import static com.simplegeek.lox.TokenType.SEMICOLON;
import static com.simplegeek.lox.TokenType.SLASH;
import static com.simplegeek.lox.TokenType.STAR;
import static com.simplegeek.lox.TokenType.STRING;
import static com.simplegeek.lox.TokenType.TRUE;
import static com.simplegeek.lox.TokenType.PRINT;
import static com.simplegeek.lox.TokenType.VAR;
import static com.simplegeek.lox.TokenType.IDENTIFIER;
import static com.simplegeek.lox.TokenType.EQUAL;

import java.util.ArrayList;
import java.util.List;

public class Parser {
	private static class ParseError extends RuntimeException {
		private static final long serialVersionUID = -2182785494034366725L;
	}
	
	private final List<Token> tokens;
	private int current = 0;
	
	public Parser(List<Token> tokens) {
		this.tokens = tokens;
	}
	
	List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<Stmt>();
		
		while (!isAtEnd()) {
			statements.add(declaration());
		}
		
		return statements;
	}
	
	private Expr expression() {
		return equality();
	}
	
	private Stmt declaration() {
		try {
			if (match(VAR)) {
				return varDeclaration();
			}
			
			return statement();
		} catch (ParseError e) {
			synchronize();
			return null;
		}
	}
	
	private Stmt statement() {
		if (match(PRINT)) {
			return printStatement();
		}
		
		return expressionStatement();
	}
	
	private Stmt printStatement() {
		Expr value = expression();
		consume(SEMICOLON, "Expect ';' after value.");
		return new Stmt.Print(value);
	}
	
	private Stmt varDeclaration() {
		Token name = consume(IDENTIFIER, "Expect variable name");
		
		Expr initializer = null;
		if (match(EQUAL)) {
			initializer = expression();
		}
		
		consume(SEMICOLON, "Expect ';' after variable declaration");
		return new Stmt.Var(name, initializer);
	}
	
	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(SEMICOLON, "Expect ';' after expression");
		return new Stmt.Expression(expr);
	}
	
	// TODO: Refactor the various Expr generating methods into one
	// more data-driven method.
	private Expr equality() {
		Expr expr = comparison();
		while (match(BANG_EQUAL, EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}
	
	private Expr comparison() {
		Expr expr = term();
		while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}
	
	private Expr term() {
		Expr expr = factor();
		while (match(MINUS, PLUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}
	
	private Expr factor() {
		Expr expr = unary();
		while (match(SLASH, STAR)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}
	
	private Expr unary() {
		if (match(BANG, MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}
		return primary();
	}
	
	private Expr primary() {
		// TODO: This needs some serious declarative
		// style refactoring - make it classy!
		if (match(FALSE)) {
			return new Expr.Literal(false);
		}
	    if (match(TRUE)) {
	    	return new Expr.Literal(true);
	    }
	    if (match(NIL)) {
	    	return new Expr.Literal(null);
	    }

	    if (match(NUMBER, STRING)) {
	      return new Expr.Literal(previous().literal);
	    }
	    
	    if (match(IDENTIFIER)) {
	    	return new Expr.Variable(previous());
	    }

	    if (match(LEFT_PAREN)) {
	      Expr expr = expression();
	      consume(RIGHT_PAREN, "Expect ')' after expression.");
	      return new Expr.Grouping(expr);
	    }
	    
	    throw error(peek(), "Expect expression");
	}
	
	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}
		return false;
	}
	
	private Token consume(TokenType type, String message) {
		if (check(type)) {
			return advance();
		}
		
		throw error(peek(), message);
	}
	
	private boolean check(TokenType type) {
		if (isAtEnd()) {
			return false;
		}
		return peek().type == type;
	}
	
	private Token advance() {
		if (!isAtEnd()) {
			current++;
		}
		return previous();
	}
	
	private boolean isAtEnd() {
		return peek().type == EOF;
	}
	
	private Token peek() {
		return tokens.get(current);
	}
	
	private Token previous() {
		return tokens.get(current - 1);
	}
	
	private ParseError error(Token token, String message) {
		Lox.error(token, message);
		return new ParseError();
	}
	
	@SuppressWarnings("incomplete-switch")
	private void synchronize() {
		advance();
		
		while(!isAtEnd()) {
			if (previous().type == SEMICOLON) {
				// This means we're at the end of a statement
				return;
			}
			
			switch (peek().type) {
			case CLASS:
			case FUN:
			case VAR:
			case FOR:
			case IF:
			case WHILE:
			case PRINT:
			case RETURN:
				return;
			}
			
			advance();
		}
	}
}
