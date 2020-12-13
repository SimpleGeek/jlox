package com.simplegeek.lox;

import com.simplegeek.lox.Expr.Binary;
import com.simplegeek.lox.Expr.Grouping;
import com.simplegeek.lox.Expr.Literal;
import com.simplegeek.lox.Expr.Unary;
import com.simplegeek.lox.Expr.Variable;

// TODO: Delete this class, once determined it is not used anymore
public class AstPrinter implements Expr.Visitor<String> {
	String print(Expr expr) {
		return expr.accept(this);
	}

	@Override
	public String visitBinaryExpr(Binary expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitGroupingExpr(Grouping expr) {
		return parenthesize("group", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Literal expr) {
		if (expr.value == null) {
			return "nil";
		}
		return expr.value.toString();
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		return parenthesize(expr.operator.lexeme, expr.right);
	}
	
	private String parenthesize(String name, Expr... exprs) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("(").append(name);
		for (Expr expr : exprs) {
			builder.append(" ");
			builder.append(expr.accept(this));
		}
		builder.append(")");
		
		return builder.toString();
	}

	@Override
	public String visitVariableExpr(Variable expr) {
		// TODO Auto-generated method stub
		return null;
	}
}
