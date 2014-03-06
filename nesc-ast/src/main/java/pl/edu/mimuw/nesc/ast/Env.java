package pl.edu.mimuw.nesc.ast;

import java.util.HashMap;

import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;

/**
 * <p>
 * Symbol table.
 * </p>
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public final class Env {

	// FIXME what when env already contains an entry with specified name?

	private final HashMap<String, DataDeclaration> table;

	public Env() {
		this.table = new HashMap<String, DataDeclaration>();
	}

	/**
	 * Returns entry associated with specified name or <code>null</code> if any
	 * entry is found.
	 * 
	 * @param name
	 *            entry's name
	 * @return entry associated with specified name or <code>null</code> if any
	 *         entry is found
	 * @throws NullPointerException
	 *             name is null
	 */
	public DataDeclaration lookup(String name) {
		if (name == null)
			throw new NullPointerException();

		return table.get(name);
	}

	/**
	 * Adds an entry with specified name.
	 * 
	 * @param name
	 *            entry's name
	 * @param declaration
	 *            entry
	 * @throws NullPointerException
	 *             name or declaration are null
	 */
	public void add(String name, DataDeclaration declaration) {
		if (name == null)
			throw new NullPointerException();
		if (declaration == null)
			throw new NullPointerException();

		table.put(name, declaration);
	}
}
