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
import static com.simplegeek.lox.TokenType.LEFT_BRACE;
import static com.simplegeek.lox.TokenType.RIGHT_BRACE;
import static com.simplegeek.lox.TokenType.IF;
import static com.simplegeek.lox.TokenType.ELSE;
import static com.simplegeek.lox.TokenType.OR;
import static com.simplegeek.lox.TokenType.AND;
import static com.simplegeek.lox.TokenType.WHILE;
import static com.simplegeek.lox.TokenType.FOR;
import static com.simplegeek.lox.TokenType.COMMA;

import java.util.ArrayList;
import java.util.Arrays;
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
		return assignment();
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
		if (match(FOR)) {
			return forStatement();
		}
		if (match(IF)) {
			return ifStatement();
		}
		if (match(PRINT)) {
			return printStatement();
		}
		if (match(WHILE)) {
			return whileStatement();
		}
		if (match(LEFT_BRACE)) {
			return new Stmt.Block(block());
		}
		
		return expressionStatement();
	}
	
	private Stmt forStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'for'.");
		
		Stmt initializer;
		if (match(SEMICOLON)) {
			initializer = null;
		} else if (match(VAR)) {
			initializer = varDeclaration();
		} else {
			initializer = expressionStatement();
		}
		
		Expr condition = null;
		if (!check(SEMICOLON)) {
			condition = expression();
		}
		consume(SEMICOLON, "Expect ';' after loop condition.");
		
		Expr increment = null;
		if (!check(RIGHT_PAREN)) {
			increment = expression();
		}
		consume(RIGHT_PAREN, "Expect ')' after for clauses.");
		
		Stmt body = statement();
		
		if (increment != null) {
			body = new Stmt.Block(
					Arrays.asList(
							body,
							new Stmt.Expression(increment)
							)
					);
		}
		
		if (condition == null) {
			condition = new Expr.Literal(true);
		}
		
		if (initializer != null) {
			body = new Stmt.Block(Arrays.asList(initializer, body));
		}
		
		body = new Stmt.While(condition, body);
		
		return body;
	}
	
	private Stmt ifStatement() {
		consume(LEFT_PAREN, "Expect '(' after the 'if'");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expect ')' after the 'if'");
		
		Stmt thenBranch = statement();
		Stmt elseBranch = null;
		if (match(ELSE)) {
			elseBranch = statement();
		}
		
		return new Stmt.If(condition, thenBranch, elseBranch);
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
	
	private Stmt whileStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'while'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expect ')' after condition.");
		Stmt body = statement();
		
		return new Stmt.While(condition, body);
	}
	
	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(SEMICOLON, "Expect ';' after expression");
		return new Stmt.Expression(expr);
	}
	
	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<Stmt>();
		
		while (!check(RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}
		
		consume(RIGHT_BRACE, "Expect '}' after block");
		return statements;
	}
	
	private Expr assignment() {
		Expr expr = or();
		
		if (match(EQUAL)) {
			Token equals = previous();
			Expr value = assignment();
			
			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable)expr).name;
				return new Expr.Assign(name, value);
			}
			
			error(equals, "Invalid assignment target");
		}
		
		return expr;
	}
	
	private Expr or() {
		Expr expr = and();
		
		while(match(OR)) {
			Token operator = previous();
			Expr right = and();
			expr = new Expr.Logical(expr, operator, right);
		}
		
		return expr;
	}
	
	private Expr and() {
		Expr expr = equality();
		
		while (match(AND)) {
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Logical(expr, operator, right);
		}
		
		return expr;
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
		return call();
	}
	
	private Expr finishCall(Expr callee) {
		List<Expr> arguments = new ArrayList<>();
		if (!check(RIGHT_PAREN)) {
			if (arguments.size() >= 255) {
				error(peek(), "Can't have more than 255 arguments");
			}
			
			do {
				arguments.add(expression());
			} while (match(COMMA));
		}
		
		Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
		
		return new Expr.Call(callee, paren, arguments);
	}
	
	private Expr call() {
		Expr expr = primary();
		while (true) {
			if (match(LEFT_PAREN)) {
				expr = finishCall(expr);
			} else {
				break;
			}
		}
		return expr;
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
