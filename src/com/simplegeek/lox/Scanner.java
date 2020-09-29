package com.simplegeek.lox;

import static com.simplegeek.lox.TokenType.BANG;
import static com.simplegeek.lox.TokenType.BANG_EQUAL;
import static com.simplegeek.lox.TokenType.COMMA;
import static com.simplegeek.lox.TokenType.DOT;
import static com.simplegeek.lox.TokenType.EOF;
import static com.simplegeek.lox.TokenType.EQUAL;
import static com.simplegeek.lox.TokenType.EQUAL_EQUAL;
import static com.simplegeek.lox.TokenType.GREATER_EQUAL;
import static com.simplegeek.lox.TokenType.IDENTIFIER;
import static com.simplegeek.lox.TokenType.LEFT_BRACE;
import static com.simplegeek.lox.TokenType.LEFT_PAREN;
import static com.simplegeek.lox.TokenType.LESS;
import static com.simplegeek.lox.TokenType.LESS_EQUAL;
import static com.simplegeek.lox.TokenType.MINUS;
import static com.simplegeek.lox.TokenType.NUMBER;
import static com.simplegeek.lox.TokenType.PLUS;
import static com.simplegeek.lox.TokenType.RIGHT_BRACE;
import static com.simplegeek.lox.TokenType.RIGHT_PAREN;
import static com.simplegeek.lox.TokenType.SEMICOLON;
import static com.simplegeek.lox.TokenType.SLASH;
import static com.simplegeek.lox.TokenType.STAR;
import static com.simplegeek.lox.TokenType.STRING;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
	private final String source;
	private final List<Token> tokens = new ArrayList<Token>();
	private int start = 0;
	private int current = 0;
	private int line = 1;
	private static final Map<String, TokenType> keywords;
	static {
		keywords = new HashMap<String, TokenType>();
		keywords.put("and", TokenType.AND);
		keywords.put("class", TokenType.CLASS);
		keywords.put("else", TokenType.ELSE);
		keywords.put("false", TokenType.FALSE);
		keywords.put("for", TokenType.FOR);
		keywords.put("fun", TokenType.FUN);
		keywords.put("if", TokenType.IF);
		keywords.put("nil", TokenType.NIL);
		keywords.put("or", TokenType.OR);
		keywords.put("print", TokenType.PRINT);
		keywords.put("return", TokenType.RETURN);
		keywords.put("super", TokenType.SUPER);
		keywords.put("this", TokenType.THIS);
		keywords.put("true", TokenType.TRUE);
		keywords.put("var", TokenType.VAR);
		keywords.put("while", TokenType.WHILE);
	}
	
	public Scanner(String source) {
		this.source = source;
	}
	
	public List<Token> scanTokens() {
		while (!isAtEnd()) {
			// We are at the beginning of the current lexeme
			start = current;
			scanToken();
		}
		
		tokens.add(new Token(EOF, "", null, line));
		return tokens;
	}
	
	private void scanToken() {
		char c = advance();
		
		switch (c) {
		case '(':
			addToken(LEFT_PAREN);
			break;
		case ')':
			addToken(RIGHT_PAREN);
			break;
		case '{':
			addToken(LEFT_BRACE);
			break;
		case '}':
			addToken(RIGHT_BRACE);
			break;
		case ',':
			addToken(COMMA);
			break;
		case '.':
			addToken(DOT);
			break;
		case '-':
			addToken(MINUS);
			break;
		case '+':
			addToken(PLUS);
			break;
		case ';':
			addToken(SEMICOLON);
			break;
		case '*':
			addToken(STAR);
			break;
		case '!':
			addToken(match('=') ? BANG_EQUAL : BANG);
			break;
		case '=':
			addToken(match('=') ? EQUAL_EQUAL : EQUAL);
			break;
		case '<':
			addToken(match('=') ? LESS_EQUAL : LESS);
			break;
		case '>':
			addToken(match('=') ? GREATER_EQUAL : EQUAL);
			break;
		case '/':
			if (match('/')) {
				// A comment goes until the end of the line, so
				// read out the rest of the line, if this line started
				// with a double slash "//".
				while (peek() != '\n' && !isAtEnd()) {
					advance();
				}
			} else {
				addToken(SLASH);
			}
			break;
		case ' ':
		case '\r':
		case '\t':
			// All these whitespace characters are meaningless
			break;
		case '\n':
			line++;
			break;
		case '"':
			string();
			break;
		
		default:
			if (isDigit(c)) {
				number();
			} else if (isAlpha(c)) {
				identifier();
			} else {
				Lox.error(line, "Unexpected character");
				break;
			}
		}
	}
	
	private void identifier() {
		while (isAlphaNumeric(peek())) {
			advance();
		}
		
		String text = source.substring(start, current);
		
		// Note that keywords are case-sensitive
		TokenType type = keywords.get(text);
		if (type == null) {
			type = IDENTIFIER;
		}
		
		addToken(type);
	}
	
	private void number() {
		// Iterating over all consecutive numeric values (0-9)
		while(isDigit(peek())) {
			advance();
		}
		
		if (peek() == '.' && isDigit(peekNext())) {
			// Consume the '.'
			advance();
			
			// Iterate over the remainder of digits after the decimal point
			while (isDigit(peek())) {
				advance();
			}
		}
		
		addToken(NUMBER,
				Double.parseDouble(source.substring(start, current)));
	}
	
	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n') {
				line++;
			}
			advance();
		}
		
		// Unterminated string ex. "unterminated
		if (isAtEnd()) {
			Lox.error(line, "Unterminated string");
			return;
		}
		
		// The closing "
		advance();
		
		// Trim surrounding quotes from lexeme
		String value = source.substring(start + 1, current - 1);
		addToken(STRING, value);
	}
	
	private boolean match(char expected) {
		if (isAtEnd()) {
			return false;
		}
		if (source.charAt(current) != expected) {
			return false;
		}
		
		current++;
		return true;
	}
	
	private char peek() {
		if (isAtEnd()) {
			return '\0';
		}
		return source.charAt(current);
	}
	
	private char peekNext() {
		if (current + 1 >= source.length()) {
			return '\0';
		}
		return source.charAt(current + 1);
	}
	
	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z')
				|| (c >= 'A' && c <= 'Z')
				|| c == '_';
	}
	
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}
	
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}
	
	private char advance() {
		current++;
		return source.charAt(current - 1);
	}
	
	private void addToken(TokenType type) {
		addToken(type, null);
	}
	
	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}
	
	private boolean isAtEnd() {
		return current >= source.length();
	}
}
