package pl.edu.mimuw.nesc.semantic.nesc;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.CString;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.NescDeclaration;
import pl.edu.mimuw.nesc.ast.gen.AstType;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.TypeArgument;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.TypeParmDecl;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.ast.gen.Word;
import pl.edu.mimuw.nesc.common.SourceLanguage;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.semantic.Semantics;

/**
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class NescSemantics {

	/**
	 * Defines internal nesc @-style attributes.
	 */
	public static void initInternalNescAttributes() {
		// TODO
		// in my opinion this should be private and called in static block.
	}

	/**
	 * Return the "identifier part" of path, i.e., remove any directory and
	 * extension
	 * 
	 * @param path
	 * @return
	 */
	public static String elementName(String path) {
		// TODO
		return null;
	}

	public static Node compile(Location location, NescDeclaration container,
			String name, boolean nameIsPath) {
		// TODO
		return null;
	}

	public static NescDeclaration load(SourceLanguage sl, Location location,
			String name, boolean nameIsPath) {
		// TODO
		return null;
	}
	
	// TODO

	public static NescDeclaration startNescEntity(SourceLanguage language,
			Word name) {
		assert (language != null);
		assert (name != null);

		NescDeclaration declaration = new NescDeclaration(language, name
				.getCstring().getData(), Semantics.getGlobalEnvironment());

		Semantics.current.startSemantics(language, declaration,
				declaration.getEnvironment());

		return declaration;
	}

	public static TypeParmDecl declareTypeParameter(Location location,
			CString id, LinkedList<Attribute> attributes, Object fixme) {
		// FIXME dd_list extra_attr
		TypeParmDecl decl = new TypeParmDecl(location, id, null);

		// TODO

		return decl;
	}

	public static Declaration declareTemplateParameter(Declarator declarator,
			LinkedList<TypeElement> elements, LinkedList<Attribute> attributes) {
		Location location = declarator != null ? declarator.getLocation()
				: elements.get(0).getLocation();
		VariableDecl variableDecl = new VariableDecl(location, declarator,
				attributes, null, null, null);
		DataDecl dataDecl = new DataDecl(location, elements,
				Lists.<Declaration> newList(variableDecl));
		return dataDecl;
	}

	public static TypeArgument makeTypeArgument(AstType astType) {
		TypeArgument typeArgument = new TypeArgument(astType.getLocation(),
				astType);
		typeArgument.setType(astType.getType());
		return typeArgument;
	}

	private NescSemantics() {
	}

}
