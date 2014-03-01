package pl.edu.mimuw.nesc.semantic;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.Type;
import pl.edu.mimuw.nesc.ast.gen.AstType;
import pl.edu.mimuw.nesc.ast.gen.DesignateField;
import pl.edu.mimuw.nesc.ast.gen.DesignateIndex;
import pl.edu.mimuw.nesc.ast.gen.Designator;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.InitList;
import pl.edu.mimuw.nesc.ast.gen.InitSpecific;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;

public class Init {

	/*
	 * TODO: ivalue_array, ivalue_field classes (in nesc-ast)
	 */

	public static void startInit(VariableDecl declaration, NescAttribute attribute) {
		// TODO
	}

	public static void finishInit() {
		// TODO
	}

	public static void simpleInit(Expression exp) {
		// TODO
	}

	public static void reallyStartIncrementalInit(Type type) {
		// TODO
	}

	public static Designator setInitIndex(Location location, Expression first, Expression last) {
		Designator designator = new DesignateIndex(location, first, last);
		// TODO
		return designator;
	}

	public static Designator setInitLabel(Location location, String fieldname) {
		Designator designator = new DesignateField(location, fieldname);
		// TODO
		return designator;
	}

	public static Expression makeInitSpecific(LinkedList<Designator> dlist, Expression initval) {
		Location location = dlist.get(0).getLocation();
		return new InitSpecific(location, dlist, initval);
	}

	public static Expression makeInitSpecific(Designator designator, Expression initval) {
		LinkedList<Designator> dlist = new LinkedList<Designator>();
		dlist.add(designator);
		return new InitSpecific(designator.getLocation(), dlist, initval);
	}

	public static Expression makeInitList(Location location, LinkedList<Expression> exps) {
		// TODO
		InitList initList = new InitList(location, exps);
		// TODO
		return initList;
	}

	private Init() {
	}
}
