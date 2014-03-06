package pl.edu.mimuw.nesc.semantic;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.CString;
import pl.edu.mimuw.nesc.ast.Environment;
import pl.edu.mimuw.nesc.ast.KnownCst;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.StorageFlag;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.TagDeclaration;
import pl.edu.mimuw.nesc.ast.Type;
import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;
import pl.edu.mimuw.nesc.ast.gen.AsmStmt;
import pl.edu.mimuw.nesc.ast.gen.AstType;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.AttributeRef;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.EnumRef;
import pl.edu.mimuw.nesc.ast.gen.Enumerator;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.FieldDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NxStructRef;
import pl.edu.mimuw.nesc.ast.gen.NxUnionRef;
import pl.edu.mimuw.nesc.ast.gen.OldIdentifierDecl;
import pl.edu.mimuw.nesc.ast.gen.PointerDeclarator;
import pl.edu.mimuw.nesc.ast.gen.QualifiedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Statement;
import pl.edu.mimuw.nesc.ast.gen.StructRef;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.UnionRef;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.ast.gen.Word;
import pl.edu.mimuw.nesc.common.util.list.Lists;

/**
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public final class Semantics {

	public static SemanticState current = new SemanticState();

	private static final Environment globalEnvironment;

	static {
		globalEnvironment = new Environment(null, true, true);
	}

	/**
	 * Returns global environment.
	 * 
	 * @return global environment
	 */
	public static Environment getGlobalEnvironment() {
		return globalEnvironment;
	}

	/**
	 * Returns the global entry with specified name or <code>null</code> if not
	 * exists.
	 * 
	 * @param name
	 *            entry's name
	 * @return an entry with specified name or <code>null</code> if not exist
	 */
	public static DataDeclaration lookupGlobalId(String name) {
		return globalEnvironment.lookupId(name, true);
	}

	public static Declarator finishArrayOrFnDeclarator(Declarator nested,
			NestedDeclarator declarator) {
		checkNotNull(nested);
		checkNotNull(declarator);
		
		declarator.setDeclarator(nested);
		return declarator;
	}

	/**
	 * Start definition of function 'elements d' with attributes attribs. nested
	 * is true for nested function definitions. Returns false in case of error.
	 * Sets current.function_decl to the declaration for this function.
	 * 
	 * @param elements
	 * @param declarator
	 * @param attribs
	 * @param nested
	 * @return
	 */
	public static boolean startFunction(LinkedList<TypeElement> elements,
			Declarator declarator, LinkedList<Attribute> attribs, boolean nested) {
		// TODO
		return true;
	}

	/**
	 * Add old-style parameter declarations old_parms to the current function.
	 * 
	 * @param oldParms
	 */
	public static void storeParmDecls(LinkedList<Declaration> oldParms) {
		// TODO
	}

	/**
	 * End definition of current function, furnishing it it's body.
	 * 
	 * @param body
	 * @return
	 */
	public static FunctionDecl finishFunction(Statement body) {
		FunctionDecl fn = null;
		// TODO
		return fn;
	}

	/**
	 * Start a new scope.
	 * 
	 * @param parmLevel
	 */
	public static void pushlevel(boolean parmLevel) {
		Environment parent = current.peekEnv();
		current.pushEnv(new Environment(parent, parmLevel, false));
	}

	/**
	 * Pop back to enclosing scope
	 * 
	 * @return
	 */
	public static Environment poplevel() {
		return current.popEnv();
	}

	/*
	 * Categories of variable declarations moved to VariableDelaration in ast.
	 */

	/**
	 * Starts definition of variable 'elements d' with attributes
	 * extra_attributes and attributes, asm specification astmt. If initialised
	 * is true, the variable has an initialiser. Returns the declaration for the
	 * variable.
	 * 
	 * @param d
	 * @param astmt
	 * @param elements
	 * @param initialised
	 * @param attributes
	 * @return
	 */
	public static Declaration startDecl(Declarator d, AsmStmt astmt,
			LinkedList<TypeElement> elements, boolean initialised,
			LinkedList<Attribute> attributes) {
		// TODO
		return null;
	}

	/**
	 * Finish definition of decl, furnishing the optional initialiser init.
	 * Returns decl.
	 * 
	 * @param decl
	 * @param init
	 */
	public static void finishDecl(Declaration decl, Expression init) {
		// TODO
	}

	/**
	 * Create definition of function parameter 'elements d' with attributes
	 * extra_attributes and attributes. Returns the declaration for the
	 * parameter.
	 * 
	 * @param declarator
	 * @param elements
	 * @param attributes
	 * @return
	 */
	public static DataDecl declareParameter(Declarator declarator,
			LinkedList<TypeElement> elements, LinkedList<Attribute> attributes) {
		Location location = (declarator != null) ? declarator.getLocation()
				: elements.get(0).getLocation();
		VariableDecl variableDecl = new VariableDecl(location, declarator,
				attributes, null, null, null);
		DataDecl dataDecl = new DataDecl(location, elements,
				Lists.<Declaration> newList(variableDecl));
		// TODO
		return dataDecl;
	}

	/**
	 * Allow parameters to be redeclared. mark_forward should be TRUE if these
	 * are "forward" parameter declarations (gcc extension).
	 * 
	 * @param parms
	 * @param mark_Forward
	 */
	public static void allowParameterRedeclaration(
			LinkedList<Declaration> parms, boolean mark_Forward) {
		// TODO
	}

	public static OldIdentifierDecl declareOldParameter(Location location,
			CString id) {
		OldIdentifierDecl decl = new OldIdentifierDecl(location, id, null);
		// TODO
		return decl;
	}

	/**
	 * Start definition of struct/union (indicated by skind) type tag.
	 * 
	 * @param location
	 * @param structKind
	 * @param tag
	 * @return
	 */
	public static TagRef startStruct(Location location, StructKind structKind,
			Word tag) {
		TagRef tagRef = makeTagRef(location, structKind, tag);
		// TODO
		return tagRef;
	}

	/**
	 * Finish definition of struct/union furnishing the fields and attribs.
	 * Returns t.
	 * 
	 * @param tagRef
	 * @param fields
	 * @param attributes
	 * @return
	 */
	public static TagRef finishStruct(TagRef tagRef,
			LinkedList<Declaration> fields, LinkedList<Attribute> attributes) {
		// TODO
		return tagRef;
	}

	/**
	 * Return a reference to struct/union/enum (indicated by skind) type tag.
	 * 
	 * @param location
	 * @param structKind
	 * @param tag
	 * @return
	 */
	public static TagRef makeXrefTag(Location location, StructKind structKind,
			Word tag) {
		TagRef tagRef = makeTagRef(location, structKind, tag);
		// TODO
		return tagRef;
	}

	/**
	 * Start definition of struct/union (indicated by skind) type tag.
	 * 
	 * @param location
	 * @param tag
	 * @return
	 */
	public static EnumRef startEnum(Location location, Word tag) {
		EnumRef enumRef = new EnumRef(location, Lists.<Attribute> newList(),
				true, Lists.<Declaration> newList(), tag);
		// TODO
		return enumRef;
	}

	/**
	 * Finish definition of enum furnishing the names and attribs. Returns t.
	 * 
	 * @param enumRef
	 * @param names
	 * @param attribs
	 * @return
	 */
	public static EnumRef finishEnum(EnumRef enumRef,
			LinkedList<Declaration> names, LinkedList<Attribute> attribs) {
		// TODO
		return enumRef;
	}

	/**
	 * Create declaration of field 'elements d : bitfield' with attributes
	 * extra_attributes and attributes. d can be NULL, bitfield can be NULL, but
	 * not both at the same time. Returns the declaration for the field.
	 * 
	 * @param declarator
	 * @param bitfield
	 * @param elements
	 * @param atributes
	 * @return
	 */
	public static FieldDecl makeField(Declarator declarator,
			Expression bitfield, LinkedList<TypeElement> elements,
			LinkedList<Attribute> atributes) {
		// FIXME: elements?
		Location location = (declarator != null) ? declarator.getLocation()
				: bitfield.getLocation();
		return new FieldDecl(location, declarator, atributes, bitfield);
	}

	public static Enumerator makeEnumerator(Location location, CString id,
			Expression value) {
		Enumerator ast = null;
		// TODO
		ast = new Enumerator(location, id, value, null);
		return ast;
	}

	/**
	 * Create and return type 'elements d' where d is an abstract declarator.
	 * 
	 * @param elements
	 * @param declarator
	 * @return
	 */
	public static AstType makeType(LinkedList<TypeElement> elements,
			Declarator declarator) {
		Location location = (elements != null && !elements.isEmpty()) ? elements.get(
				0)
				.getLocation()
				: declarator.getLocation();

		AstType type = new AstType(location, declarator, elements);
		// TODO
		return type;
	}

	/**
	 * If statement list l1 ends with an unfinished label, attach l2 to that
	 * label. Otherwise attach l2 to the end of l1.
	 * 
	 * @param l1
	 * @param l2
	 * @return
	 */
	public static LinkedList<Statement> chainWithLabels(
			LinkedList<Statement> l1, LinkedList<Statement> l2) {
		assert l1 != null;
		assert l2 != null;

		if (l1.isEmpty())
			return l2;
		if (l2.isEmpty())
			return l1;

		// TODO

		l1.addAll(l2);
		return l1;
	}

	/*
	 * TODO void declarator_name(declarator d, const char **oname, const char
	 * **iname); const char *nice_declarator_name(declarator d);
	 */

	public static DataDeclaration implicitlyDeclare(Identifier fnid) {
		// TODO
		return null;
	}

	public static void pushLabelLevel() {
		// TODO
	}

	public static void popLabelLevel() {
		// TODO
	}

	public static void initDataDeclaration(DataDeclaration dd, Declaration ast,
			String name, Type t) {
		// TODO
	}

	public static Declarator finishFunctionDeclarator(
			FunctionDeclarator declarator) {
		// TODO

		return declarator;
	}

	public static Declarator makePointerDeclarator(Location location,
			Declarator declarator, LinkedList<TypeElement> quals) {
		declarator = new QualifiedDeclarator(location, declarator, quals);
		return new PointerDeclarator(location, declarator);
	}

	public static int duplicateDecls(DataDeclaration newdecl,
			DataDeclaration olddecl, boolean differentBindingLevel,
			boolean newinitialised) {
		// TODO
		return 0;
	}

	public static void checkVariableScflags(StorageFlag scf, Location location,
			String kind, String name) {
		// TODO scf - enumset
		// TODO
	}

	public static void parseDeclarator(LinkedList<TypeElement> modifiers,
			Declarator d, boolean bitfield, boolean requireParmNames,
			Integer oclass, StorageFlag oscf, String[] ointf, String[] oname,
			Type ot, Boolean owarnDefaultedInt,
			FunctionDeclarator ofunction_declarator,
			LinkedList<Attribute> oattributes) {
		// TODO oclass, ocsf <- out params, oscf should be EnumSet
		// TODO
	}

	public static void checkArraySize(Expression size, String printName) {
		// TODO
	}

	public static void layoutEnumStart(TagDeclaration tdecl) {
		// TODO
	}

	public static void layoutEnumEnd(TagDeclaration tdecl) {
		// TODO
	}

	public static KnownCst layoutEnumValue(Enumerator e) {
		// TODO
		return null;
	}

	public static void layoutStruct(TagDeclaration tdecl) {
		// TODO
	}

	public static String tagkindName(int tagkind) {
		// TODO
		// FIXME int -> enum
		return null;
	}

	public static boolean handleModeattribute(Location location,
			DataDeclaration ddecl, String mode) {
		// TODO
		return false;
	}

	/*
	 * Make "word" argument of attributes into an expression
	 */
	public static LinkedList<Expression> makeAttrArgs(Location location,
			CString id, LinkedList<Expression> args) {
		// args may be null
		// FIXME bad_decl
		Identifier identifier = new Identifier(location, id, null);
		// bad_decl result->type = error_type;
		if (args != null) {
			args.addFirst(identifier);
		} else {
			args = Lists.<Expression>newList(identifier);
		}
		return args;
	}

	public static DataDeclaration declareBuiltinType(String name, Type t) {
		// TODO
		return null;
	}

	private static LinkedList<Attribute> checkParameter(DataDeclaration dd,
			LinkedList<TypeElement> elements, VariableDecl vd) {
		return null;
	}

	private static TagRef makeTagRef(Location location, StructKind structKind,
			Word tag) {
		switch (structKind) {
		case STRUCT:
			return new StructRef(location, Lists.<Attribute> newList(), true,
					Lists.<Declaration> newList(), tag);
		case UNION:
			return new UnionRef(location, Lists.<Attribute> newList(), true,
					Lists.<Declaration> newList(), tag);
		case NX_STRUCT:
			return new NxStructRef(location, Lists.<Attribute> newList(), true,
					Lists.<Declaration> newList(), tag);
		case NX_UNION:
			return new NxUnionRef(location, Lists.<Attribute> newList(), true,
					Lists.<Declaration> newList(), tag);
		case ENUM:
			return new EnumRef(location, Lists.<Attribute> newList(), true,
					Lists.<Declaration> newList(), tag);
		case ATTRIBUTE:
			return new AttributeRef(location, Lists.<Attribute> newList(),
					true, Lists.<Declaration> newList(), tag);
		default:
			throw new IllegalArgumentException("Unexpected argument "
					+ structKind);
		}
	}
	
	private Semantics() {
	}

}
