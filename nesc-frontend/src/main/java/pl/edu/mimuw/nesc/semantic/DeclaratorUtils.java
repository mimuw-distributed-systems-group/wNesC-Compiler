package pl.edu.mimuw.nesc.semantic;

import pl.edu.mimuw.nesc.ast.gen.ArrayDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.GenericDeclarator;
import pl.edu.mimuw.nesc.ast.gen.IdentifierDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.PointerDeclarator;
import pl.edu.mimuw.nesc.ast.gen.QualifiedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Visitor;

import com.google.common.base.Preconditions;

/**
 * Common operations on declarators.
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public final class DeclaratorUtils {

	private static final DeclaratorNameVisitor DECLARATOR_NAME_VISITOR = new DeclaratorNameVisitor();
	private static final IsFunctionDeclaratorVisitor IS_FUNCTION_DECLARATOR_VISITOR = new IsFunctionDeclaratorVisitor();

	/**
	 * Gets declarator's name.
	 * 
	 * @param declarator
	 *            declarator
	 * @return declarator's name
	 */
	public static String getDeclaratorName(Declarator declarator) {
		Preconditions.checkNotNull(declarator, "declarator cannot be null");
		return declarator.accept(DECLARATOR_NAME_VISITOR, null);
	}

	public static boolean isFunctionDeclarator(Declarator declarator) {
		Preconditions.checkNotNull(declarator, "declarator cannot be null");
		return declarator.accept(IS_FUNCTION_DECLARATOR_VISITOR, null);
	}

	private DeclaratorUtils() {
	}

	/**
	 * Visitor for extracting declarator's name.
	 * 
	 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
	 * 
	 */
	private static class DeclaratorNameVisitor extends Visitor<String, Void> {

		@Override
		public String visitDeclarator(Declarator elem, Void arg) {
			throw new IllegalStateException("Declarator object must not be instantiated.");
		}

		@Override
		public String visitNestedDeclarator(NestedDeclarator elem, Void arg) {
			return elem.getDeclarator().accept(this, null);
		}

		@Override
		public String visitFunctionDeclarator(FunctionDeclarator elem, Void arg) {
			return elem.getDeclarator().accept(this, null);
		}

		@Override
		public String visitPointerDeclarator(PointerDeclarator elem, Void arg) {
			return elem.getDeclarator().accept(this, null);
		}

		@Override
		public String visitQualifiedDeclarator(QualifiedDeclarator elem, Void arg) {
			return elem.getDeclarator().accept(this, null);
		}

		@Override
		public String visitArrayDeclarator(ArrayDeclarator elem, Void arg) {
			return elem.getDeclarator().accept(this, null);
		}

		@Override
		public String visitIdentifierDeclarator(IdentifierDeclarator elem, Void arg) {
			return elem.getCstring().getData();
		}

		@Override
		public String visitGenericDeclarator(GenericDeclarator elem, Void arg) {
			return elem.getDeclarator().accept(this, null);
		}

		@Override
		public String visitInterfaceRefDeclarator(InterfaceRefDeclarator elem, Void arg) {
			return elem.getWord1().getCstring().getData();
		}

	}

	private static class IsFunctionDeclaratorVisitor extends Visitor<Boolean, Void> {

		@Override
		public Boolean visitDeclarator(Declarator elem, Void arg) {
			throw new IllegalStateException("Declarator object must not be instantiated.");
		}

		@Override
		public Boolean visitNestedDeclarator(NestedDeclarator elem, Void arg) {
			return Boolean.FALSE;
		}

		@Override
		public Boolean visitFunctionDeclarator(FunctionDeclarator elem, Void arg) {
			return Boolean.TRUE;
		}

		@Override
		public Boolean visitPointerDeclarator(PointerDeclarator elem, Void arg) {
			return Boolean.FALSE;
		}

		@Override
		public Boolean visitQualifiedDeclarator(QualifiedDeclarator elem, Void arg) {
			return Boolean.FALSE;
		}

		@Override
		public Boolean visitArrayDeclarator(ArrayDeclarator elem, Void arg) {
			return Boolean.FALSE;
		}

		@Override
		public Boolean visitIdentifierDeclarator(IdentifierDeclarator elem, Void arg) {
			return Boolean.FALSE;
		}

		@Override
		public Boolean visitGenericDeclarator(GenericDeclarator elem, Void arg) {
			return Boolean.FALSE;
		}

		@Override
		public Boolean visitInterfaceRefDeclarator(InterfaceRefDeclarator elem, Void arg) {
			return Boolean.FALSE;
		}

	}

}
