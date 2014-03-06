package pl.edu.mimuw.nesc.semantic;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.FieldDeclaration;
import pl.edu.mimuw.nesc.ast.NescDeclaration;
import pl.edu.mimuw.nesc.ast.TagDeclaration;
import pl.edu.mimuw.nesc.ast.Type;
import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.GccAttribute;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;

public final class Attributes {

	/* Provide warnings about ignored attributes and attribute lists */

	public static void ignoredAttribute(Attribute attr) {
		// TODO
	}

	public static void ignoredAttributes(LinkedList<Attribute> alist) {
		// TODO
	}

	public static void ignoredDdAttributes(LinkedList<Attribute> alist) {
		// TODO
	}

	public static void ignored_gcc_attribute(GccAttribute attr) {
		// TODO
	}

	public static void ignored_nesc_attribute(NescAttribute attr) {
		// TODO
	}

	/*
	 * handle_X_attribute(attr, obj): Attempt to apply attribute attr to obj of
	 * kind X (decl, field, tag, type), modifying obj.
	 * 
	 * For decls, fields, tags: If attr is not applicable: issue a warning with
	 * ignored_attributes For types: Return TRUE if applicable, FALSE if not
	 * (this difference is due to the funky rules of attributes used as type
	 * qualifiers)
	 */

	public static void handleNescdeclAttribute(Attribute attr, NescDeclaration ndecl) {
		// TODO
	}

	public static void handleDeclAttribute(Attribute attr, DataDeclaration ddecl) {
		// TODO
	}

	public static void handleFieldAttribute(Attribute attr, FieldDeclaration fdecl) {
		// TODO
	}

	public static void handleTagAttribute(Attribute attr, TagDeclaration tdecl) {
		// TODO
	}

	public static boolean handleTypeAttribute(Attribute attr, Type type) {
		// TODO
		return false;
	}

	/*
	 * Functions to handle regular and dd list of attributes
	 */

	public static void handleNescdeclAttributes(LinkedList<Attribute> alist, NescDeclaration ndecl) {
		// TODO
	}

	public static void handleDeclAttributes(LinkedList<Attribute> alist, DataDeclaration ddecl) {
		// TODO
	}

	public static void handleFieldAttributes(LinkedList<Attribute> alist, FieldDeclaration fdecl) {
		// TODO
	}

	public static void handleTagAttributes(LinkedList<Attribute> alist, TagDeclaration tdecl) {
		// TODO
	}

	public static void handleNescdeclDdAttributes(LinkedList<Attribute> alist, NescDeclaration ndecl) {
		// TODO
	}

	public static void handleDeclDdAttributes(LinkedList<Attribute> alist, DataDeclaration ddecl) {
		// TODO
	}

	public static void handleFieldDdAttributes(LinkedList<Attribute> alist, FieldDeclaration fdecl) {
		// TODO
	}

	public static void handleTagDdAttributes(LinkedList<Attribute> alist, TagDeclaration tdecl) {
		// TODO
	}

	private Attributes() {
		throw new RuntimeException("Object of this class should never be created");
	}

}
