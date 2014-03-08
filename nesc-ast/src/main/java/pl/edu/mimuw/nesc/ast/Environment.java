package pl.edu.mimuw.nesc.ast;

import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;

/**
 * <p>
 * Environment keeps symbol tables for current scope and its predecessors.
 * </p>
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public final class Environment {

	/*
	 * This implementation of environment differs slightly from original. The
	 * main difference is the manner we do the lookup for specified identifier.
	 * The sub-tables {@link Env} for tags/ids do not know their parents. When
	 * the symbol is not found in current scope, we go to the preceding {@link
	 * Environment} and then to the proper sub-able instead of directly to
	 * corresponding parent {@link Env}.
	 */

	// TODO: deputy_scope

	private final Environment parent;
	private final FunctionDecl fdecl;
	private final boolean parmLevel;
	/**
	 * Both system and component has a globalLevel equals <code>true</code>.
	 */
	private boolean globalLevel;
	private final Env idEnv;
	private final Env tagEnv;

	/**
	 * Creates new <code>Environment</code>.
	 * 
	 * @param parent
	 *            scope which new scope is enclosed in
	 * @param fdecl
	 *            TODO
	 * @param parmLevel
	 *            TODO
	 * @param globalLevel
	 *            is it a global scope (both system and component)
	 */
	public Environment(Environment parent, boolean parmLevel, boolean globalLevel) {
		this.parent = parent;
		this.parmLevel = parmLevel;
		this.globalLevel = globalLevel;

		if (parent != null) {
			this.fdecl = parent.fdecl;
			this.idEnv = new Env();

			if (parent.isParmLevel()) {
				tagEnv = parent.getTagEnv();
			} else {
				tagEnv = new Env();
			}
		} else {
			this.fdecl = null;
			this.idEnv = new Env();
			this.tagEnv = new Env();
		}
	}

	public FunctionDecl getFdecl() {
		return fdecl;
	}

	public boolean isParmLevel() {
		return parmLevel;
	}

	public boolean isGlobalLevel() {
		return globalLevel;
	}

	public void setGlobalLevel(boolean globalLevel) {
		this.globalLevel = globalLevel;
	}

	public Env getIdEnv() {
		return idEnv;
	}

	public Env getTagEnv() {
		return tagEnv;
	}

}
