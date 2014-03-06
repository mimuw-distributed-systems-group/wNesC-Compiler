package pl.edu.mimuw.nesc.semantic;

import java.util.Stack;

import pl.edu.mimuw.nesc.ast.Environment;
import pl.edu.mimuw.nesc.ast.NescDeclaration;
import pl.edu.mimuw.nesc.ast.gen.AtomicStmt;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.common.SourceLanguage;

/**
 * <p>
 * SemanticState keeps data that is essential during parsing state but cannot be
 * obtained directly from grammar rule's elements.
 * </p>
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class SemanticState {

	/**
	 * Specifies within which component specification section we are.
	 * 
	 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
	 * 
	 */
	public static enum SpecSection {
		/**
		 * Neither in <code>provides</code> nor in <code>uses</code> section.
		 */
		NORMAL,
		/**
		 * In a <code>provides</code> section.
		 */
		PROVIDES,
		/**
		 * In a <code>uses</code> section.
		 */
		USES;
	}

	/**
	 * The current language.
	 */
	public SourceLanguage language;
	/**
	 * The nesc entity of the file being compiled.
	 */
	public NescDeclaration file;
	/**
	 * The nesc entity being compiled (null for C).
	 */
	public NescDeclaration container;
	/**
	 * The function currently being defined.
	 */
	public Stack<FunctionDecl> functionDecl;
	/**
	 * TODO
	 */
	public TagRef pendingInvalidXref;
	/**
	 * The current component specification section.
	 */
	public SpecSection specSection;
	/**
	 * The current environment.
	 */
	private Stack<Environment> env;
	/**
	 * The lexically containing atomic statement.
	 */
	private Stack<AtomicStmt> inAtomic;

	public SemanticState() {
		this.env = new Stack<Environment>();
		this.env.push(new Environment(null, false, true));
		this.inAtomic = new Stack<AtomicStmt>();
	}

	/**
	 * TODO
	 * 
	 * @param language
	 * @param container
	 * @param env
	 */
	public void startSemantics(SourceLanguage language,
			NescDeclaration container, Environment env) {
		this.env.push(env);
		this.language = language;
		this.container = container;
	}

	/**
	 * Returns <code>true</code> if environment stack is not empty.
	 * 
	 * @return <code>true</code> if environment stack is not empty
	 */
	public boolean isEnvEmpty() {
		return !this.env.isEmpty();
	}

	/**
	 * Pushes environment on the environment stack.
	 * 
	 * @param atomicStmt
	 *            environment
	 */
	public void pushEnv(Environment env) {
		this.env.push(env);
	}

	/**
	 * Returns and removes environment from the environment stack.
	 * 
	 * @return environment
	 */
	public Environment popEnv() {
		return this.env.pop();
	}

	/**
	 * Returns environment from the environment stack.
	 * 
	 * @return environment
	 */
	public Environment peekEnv() {
		return this.env.peek();
	}

	/**
	 * Returns <code>true</code> if current statement is enclosed in atomic
	 * statement.
	 * 
	 * @return <code>true</code> if current statement is enclosed in atomic
	 *         statement
	 */
	public boolean isInAtomic() {
		return !this.inAtomic.isEmpty();
	}

	/**
	 * Pushes atomic statement on the atomic statements stack.
	 * 
	 * @param atomicStmt
	 *            current atomic statement
	 */
	public void pushInAtomic(AtomicStmt atomicStmt) {
		this.inAtomic.push(atomicStmt);
	}

	/**
	 * Returns and removes atomic statement from the atomic statements stack.
	 * 
	 * @return enclosing atomic statement
	 */
	public AtomicStmt popInAtomic() {
		return this.inAtomic.pop();
	}

	/**
	 * Returns atomic statement from the atomic statements stack.
	 * 
	 * @return enclosing atomic statement
	 */
	public AtomicStmt peekInAtomic() {
		return this.inAtomic.peek();
	}

}
