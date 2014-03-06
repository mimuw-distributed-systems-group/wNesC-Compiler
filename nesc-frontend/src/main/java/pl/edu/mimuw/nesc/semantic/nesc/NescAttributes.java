package pl.edu.mimuw.nesc.semantic.nesc;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.FieldDeclaration;
import pl.edu.mimuw.nesc.ast.IValue;
import pl.edu.mimuw.nesc.ast.NescDeclaration;
import pl.edu.mimuw.nesc.ast.TagDeclaration;
import pl.edu.mimuw.nesc.ast.Type;
import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;
import pl.edu.mimuw.nesc.ast.gen.Word;

public class NescAttributes {

	public static void InitNescAttributes() {
		// TODO
	}

	public static NescAttribute startAttributeUse(Word name) {
		NescAttribute attr = new NescAttribute(name.getLocation(), name, null);
		// TODO
		return attr;
	}

	public static Attribute finishAttributeUse(NescAttribute attr, LinkedList<Expression> init) {
		// TODO
		return null;
	}

	/*
	 * TODO define_internal_attriubte
	 */

	/*
	 * Returns: The initialiser for field name in attr, or NULL if it's not
	 * found
	 */

	public static IValue lookupAttributeField(NescAttribute attr, String name) {
		// TODO
		return null;
	}

	public static void handleNescTypeAttribute(NescAttribute attr, Type t) {
		// TODO
	}

	public static void handleNescDeclAttribute(NescAttribute attr, DataDeclaration ddecl) {
		// TODO
	}

	public static void handleNescFieldAttribute(NescAttribute attr, FieldDeclaration fdecl) {
		// TODO
	}

	public static void handleNescTagAttribute(NescAttribute attr, TagDeclaration tdecl) {
		// TODO
	}

	public static void handleNescNescdeclAttribute(NescAttribute attr, NescDeclaration ndecl) {
		// TODO
	}

}
