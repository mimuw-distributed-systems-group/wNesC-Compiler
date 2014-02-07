package pl.edu.mimuw.nesc.parser;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.google.common.base.Preconditions;

/**
 * Symbol table containing pairs <tt>(name, type)</tt> with regard to scopes,
 * where type indicates whether identifier is type name, component reference or
 * plain identifier.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 *
 */
public final class SymbolTable {

	/**
	 * Contains definition names and types in current scope.
	 *
	 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
	 *
	 */
	public static class Scope {

		public static Scope ofGlobalScope() {
			return new Scope(null, false);
		}

		private final Scope parent;
		private final HashMap<String, Integer> definitions;
		private final boolean isParmLevel;

		public Scope(Scope parent, boolean isParmLevel) {
			this.parent = parent;
			this.definitions = new HashMap<>();
			this.isParmLevel = isParmLevel;
		}

		public void add(String name, int type) {
			this.definitions.put(name, type);
		}

		public Integer get(String name) {
			return this.definitions.get(name);
		}

        public Map<String, Integer> getAll() {
            return this.definitions;
        }

		public Scope getParent() {
			return this.parent;
		}

		public boolean isParmLevel() {
			return this.isParmLevel;
		}

		@Override
		public String toString() {
			return "{ Scope; {parent=" + parent + ", definitions=" + definitions + ", isParmLevel=" + isParmLevel
					+ "}}";
		}

	}

	private final Stack<Scope> scopes;

	public SymbolTable() {
		this(new Scope(null, false));
	}

	public SymbolTable(Scope globalScope) {
		Preconditions.checkNotNull(globalScope, "global scope cannot be null");
		this.scopes = new Stack<>();
		this.scopes.add(globalScope);
	}

	public void popLevel() {
		this.scopes.pop();
	}

	public void pushLevel(boolean isParmLevel) {
		// FIXME isParmLevel?
		final Scope parent = this.scopes.peek();
		final Scope current = new Scope(parent, isParmLevel);
		this.scopes.push(current);
	}

	public int get(String name) {
		checkNotNull(name);

		Scope current = this.scopes.peek();
		while (current != null) {
			final Integer type = current.get(name);
			if (type != null) {
				return type;
			}
			current = current.getParent();
		}
		// by default return plain identifier
		return Parser.Lexer.IDENTIFIER;
	}

	public void addIdentifier(String name) {
		checkNotNull(name, "name cannot be null");
		add(name, Parser.Lexer.IDENTIFIER);
	}

	public void addTypename(String name) {
		checkNotNull(name, "name cannot be null");
		add(name, Parser.Lexer.TYPEDEF_NAME);
	}

	public void addComponentRef(String name) {
		checkNotNull(name, "name cannot be null");
		add(name, Parser.Lexer.COMPONENTREF);
	}

	public void add(String name, int type) {
		this.scopes.peek().add(name, type);
	}

    public boolean isGlobalLevel() {
        return this.scopes.size() == 1;
    }

	@Override
	public String toString() {
		return "{ SymbolTable; {scopes=" + scopes + "}}";
	}

}
