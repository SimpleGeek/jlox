package com.simplegeek.lox;

public class Debug {
	// This class is only for debugging some specific pieces of code.
	// Any actual functionality testing that should be used later should
	// be added to some sort of honest-to-goodness unit test.
	public static void main(String[] args) {
		Expr expression = new Expr.Binary(
				new Expr.Unary(	new Token(TokenType.MINUS, "-", null, 1),
								new Expr.Literal(123)),
				new Token(TokenType.STAR, "*", null, 1),
				new Expr.Grouping(new Expr.Literal(45.67))
				);
		
		System.out.println(new AstPrinter().print(expression));
	}
}
