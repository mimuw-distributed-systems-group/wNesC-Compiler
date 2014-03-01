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

	public static ReturnStmt makeReturn(Location location, Expression expression) {
		ReturnStmt returnStmt = new ReturnStmt(location, expression);
		return returnStmt;
	}

	public static ReturnStmt makeVoidReturn(Location location) {
		ReturnStmt returnStmt = new ReturnStmt(location, null);
		return returnStmt;
	}

	public static void useLabel(IdLabel label) {
		/*
		 * TODO set label as used. Check whether label refers to allowed place.
		 * stmt.c 190
		 */
	}

	public static ErrorStmt makeErrorStmt() {
		return new ErrorStmt(null); // FIXME use dummy location.
	}

}
