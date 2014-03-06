package pl.edu.mimuw.nesc.parser;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.CString;
import pl.edu.mimuw.nesc.ast.Environment;
import pl.edu.mimuw.nesc.ast.LeftUnaryOperation;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.NescCallKind;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.TagDeclaration;
import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;
import pl.edu.mimuw.nesc.ast.gen.AsmOperand;
import pl.edu.mimuw.nesc.ast.gen.AsmStmt;
import pl.edu.mimuw.nesc.ast.gen.AstType;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.Component;
import pl.edu.mimuw.nesc.ast.gen.ComponentRef;
import pl.edu.mimuw.nesc.ast.gen.ConditionalStmt;
import pl.edu.mimuw.nesc.ast.gen.Connection;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.Designator;
import pl.edu.mimuw.nesc.ast.gen.EndPoint;
import pl.edu.mimuw.nesc.ast.gen.EnumRef;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.ForStmt;
import pl.edu.mimuw.nesc.ast.gen.FunctionCall;
import pl.edu.mimuw.nesc.ast.gen.GccAttribute;
import pl.edu.mimuw.nesc.ast.gen.IdLabel;
import pl.edu.mimuw.nesc.ast.gen.Implementation;
import pl.edu.mimuw.nesc.ast.gen.Interface;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRef;
import pl.edu.mimuw.nesc.ast.gen.Label;
import pl.edu.mimuw.nesc.ast.gen.LexicalCst;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.ParameterisedIdentifier;
import pl.edu.mimuw.nesc.ast.gen.Qualifier;
import pl.edu.mimuw.nesc.ast.gen.Rid;
import pl.edu.mimuw.nesc.ast.gen.Statement;
import pl.edu.mimuw.nesc.ast.gen.StringAst;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.ast.gen.Word;

/**
 * <p>
 * Simulates union from C/C++. Yacc uses a union to pass result of each
 * production. The production's result may have different types, depending on
 * production (e.g. int, string, expr, stmt), so that the object returned has
 * many different fields: one for each possible returned object type.
 * </p>
 * 
 * <p>
 * Java language does not provide union, but still we define a field for each
 * possible type of returned object.
 * </p>
 * 
 * <p>
 * Parser demands fields to be . It does not access them using setters and
 * getters.
 * </p>
 * 
 * <p>
 * FIXME: irrelevant since bison parser generator is used. Refactoring needed.
 * </p>
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class Value {

	public static class StructKindToken {
		Location location;
		StructKind kind;
	}

	public static class IToken {
		Location location;
		int i;
	}

	public static class IExpr {
		Expression expr;
		int i;
	}

	public static class IStmt {
		Statement stmt;
		/**
		 * <p>
		 * Statements counter.
		 * </p>
		 * TODO
		 */
		int i;
	}

	public static class IStmts {
		LinkedList<Statement> stmts;
		int i;
	}

	public static class IdToken {
		Location location;
		CString id;
		DataDeclaration decl;

		public IdToken() {
		}

		public IdToken(String str) {
			this.id = new CString(str);
		}

		@Override
		public String toString() {
			return id.getData();
		}

	}

	/*
	 * Plain values.
	 */
	String string;
	LinkedList<String> strings;
	int integer;
	boolean bool;

	AsmOperand asmOperand;
	LinkedList<AsmOperand> asmOperands;
	AsmStmt asmStmt;
	Attribute attribute;
	LinkedList<Attribute> attributes;
	GccAttribute gccAttribute;
	NescAttribute nescAttribute;
	LexicalCst constant;
	Declaration decl;
	LinkedList<Declaration> decls;
	VariableDecl variableDecl;
	Declarator declarator;
	LinkedList<Declarator> declarators;
	NestedDeclarator nested;
	Expression expr;
	LinkedList<Expression> exprs;
	FunctionCall functionCall;
	IdLabel idLabel;
	LinkedList<IdLabel> idLabels;
	Label label;
	LinkedList<Label> labels;
	Node node;
	Statement stmt;
	LinkedList<Statement> stmts;
	ConditionalStmt cstmt;
	ForStmt forStmt;
	StringAst stringAst;
	LinkedList<StringAst> stringsAst;
	TypeElement telement;
	LinkedList<TypeElement> telements;
	TagRef tagRef;
	EnumRef enumRef;
	Rid rid;
	Qualifier qualifier;
	AstType type;
	Word word;
	Designator designator;
	LinkedList<Designator> designators;
	InterfaceRef iref;
	ComponentRef cref;
	LinkedList<ComponentRef> crefs;
	Connection conn;
	EndPoint endPoint;
	ParameterisedIdentifier pid;
	Implementation impl;
	Environment env;
	TagDeclaration tdecl;
	Interface intf;
	Component component;

	NescCallKind callKind;
	LeftUnaryOperation leftUnaryOperation;

	StructKindToken structKindToken = new StructKindToken();
	IToken itoken = new IToken();
	IExpr iexpr = new IExpr();
	IStmt istmt = new IStmt();
	IStmts istmts = new IStmts();

	IdToken idtoken = new IdToken();

}
