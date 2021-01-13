package com.simplegeek.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * This class will generate the AST classes with the
 * com.simplegeek.lox.Expr.java file.  It's best to run
 * it by just manually creating a run configuration within
 * your IDE that points to this class as the main class.
 * Note that you'll probably want to run an eclipse format 
 * over the finished file; the generated version isn't exactly gorgeous.
 */
public class GenerateAst {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: generate_ast <output directory>");
			System.exit(64);
		}
		String outputDir = args[0];
		defineAst(outputDir, "Expr", Arrays.asList(
				"Assign	  : Token name, Expr value",
				"Binary   : Expr left, Token operator, Expr right",
				"Call     : Expr callee, Token paren, List<Expr> arguments",
				"Grouping : Expr expression",
				"Literal  : Object value",
				"Logical  : Expr left, Token operator, Expr right",
				"Unary    : Token operator, Expr right",
				"Variable : Token name"
				));
		
		defineAst(outputDir, "Stmt", Arrays.asList(
				"Block		: List<Stmt> statements",
				"Expression : Expr expression",
				"If			: Expr condition, Stmt thenBranch," + " Stmt elseBranch",
				"Print		: Expr expression",
				"Var		: Token name, Expr initializer",
				"While		: Expr condition, Stmt body"
				));
		System.out.println("Completed generation of AST classes");
	}
	
	private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
		String path = outputDir + "/" + baseName + ".java";
		PrintWriter writer = new PrintWriter(path, "UTF-8");
		
		// Beginning class definition - required prior to any subclasses
		writer.println("package com.simplegeek.lox;");
		writer.println();
		writer.println("public abstract class " + baseName + " {");
		
		defineVisitor(writer, baseName, types);
		
		// Generating the AST classes
		for (String type : types) {
			String className = type.split(":")[0].trim();
			String fields = type.split(":")[1].trim();
			defineType(writer, baseName, className, fields);
		}
		
		// The base accept() method
		writer.println();
		writer.println("	abstract <R> R accept(Visitor<R> visitor);");
		
		// Finishing the class definition
		writer.println("}");
		writer.close();
	}
	
	private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
		writer.println("	interface Visitor<R> {");
		
		for (String type : types) {
			String typeName = type.split(":")[0].trim();
			writer.println("	R visit" + typeName + baseName + "(" +
							typeName + " " + baseName.toLowerCase() + ");");
		}
		
		writer.println("	}");
	}
	
	private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
		writer.println("	static class " + className + " extends " + baseName + " {");
		
		// Constructor
		writer.println("		" + className + "(" + fieldList + ") {");
		String[] fields = fieldList.split(", ");
		for (String field : fields) {
			String name = field.split(" ")[1];
			writer.println("			this." + name + " = " + name + ";");
		}
		writer.println("		}");
		
		// Visitor pattern
		writer.println();
		writer.println("		@Override");
		writer.println("		<R> R accept(Visitor<R> visitor) {");
		writer.println("		return visitor.visit" +
						className + baseName + "(this);");
		writer.println("		}");
		
		// Fields
		writer.println();
		for (String field : fields) {
			writer.println("		final " + field + ";");
		}
		
		// Finish class definition
		writer.println("	}");
	}
}
