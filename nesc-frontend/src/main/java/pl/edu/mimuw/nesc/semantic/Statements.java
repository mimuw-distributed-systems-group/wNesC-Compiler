package pl.edu.mimuw.nesc.semantic;

import pl.edu.mimuw.nesc.ast.LabelDeclaration;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.ErrorStmt;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.IdLabel;
import pl.edu.mimuw.nesc.ast.gen.Label;
import pl.edu.mimuw.nesc.ast.gen.ReturnStmt;
import pl.edu.mimuw.nesc.ast.gen.Statement;

/**
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class Statements {

	public static void checkCondition(String context, Expression e) {
		// TODO
	}

	public static void checkSwitch(Expression e) {
		// TODO
	}

	public static void checkVoidReturn() {
		// TODO
	}

	public static void checkReturn(Expression e) {
		// TODO
	}

	public static void checkComputedGoto(Expression e) {
		// TODO
	}

	public static ReturnStmt makeReturn(Location location, Expression expression) {
		ReturnStmt returnStmt = new ReturnStmt(location, expression);
		// TODO
		if (Semantics.current.isInAtomic())
			returnStmt.setContainingAtomic(Semantics.current.popInAtomic());
		// TODO
		return returnStmt;
	}

	public static ReturnStmt makeVoidReturn(Location location) {
		// TODO
		ReturnStmt returnStmt = new ReturnStmt(location, null);
		if (Semantics.current.isInAtomic()) {
			returnStmt.setContainingAtomic(Semantics.current.popInAtomic());
		}
		return returnStmt;
	}

	public static void lookupLabel(IdLabel label) {
		// TODO
	}

	public static void useLabel(IdLabel label) {
		/*
		 * TODO set label as used. Check whether label refers to allowed place.
		 * stmt.c 190
		 */
	}

	public static void defineLabel(IdLabel label) {
		// TODO
	}

	public static void declareLabel(IdLabel label) {
		// TODO
	}

	public static void checkLabels() {
		// TODO
	}

	public static void checkCase(Label caseLabel) {
		// TODO
	}

	public static void checkCaseValue(Expression e) {
		// TODO
	}

	public static void checkDefault(Label defaultLabel) {
		// TODO
	}

	public static void checkBreak(Statement breakStatement) {
		// TODO
	}

	public static void checkContinue(Statement continueStatement) {
		// TODO
	}

	public static void failInAtomic(String context) {
		// TODO
	}

	public static void pushLoop(Statement loopStatement) {
		// TODO
	}

	public static void popLoop() {
		// TODO
	}

	public static LabelDeclaration newLabelDelaration(String name,
			IdLabel firstUse) {
		// TODO
		return null;
	}

	public static ErrorStmt makeErrorStmt() {
		return new ErrorStmt(null); // FIXME use dummy location.
	}

}
