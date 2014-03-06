package pl.edu.mimuw.nesc;

import static com.google.common.base.Preconditions.checkNotNull;
import pl.edu.mimuw.nesc.parser.ParseOrderResolver;
import pl.edu.mimuw.nesc.parser.SymbolTable.Scope;
import pl.edu.mimuw.nesc.preprocessor.MacrosManager;

public final class ParseContext {

	private final PathsResolver pathsResolver;
	private final ParseOrderResolver orderResolver;
	private final MacrosManager macrosManager;
	/**
	 * Global scope of symbol table that remembers if identifier is a typename,
	 * componentref or plain identifier.
	 */
	private final Scope globalScope;
	private final ParseResult parseResult;

	public ParseContext(PathsResolver pathsResolver, ParseOrderResolver orderResolver, MacrosManager macrosManager) {
		checkNotNull(pathsResolver, "paths resolver cannot be null");
		checkNotNull(orderResolver, "order resolver cannot be null");
		checkNotNull(macrosManager, "macros manager cannot be null");

		this.pathsResolver = pathsResolver;
		this.orderResolver = orderResolver;
		this.macrosManager = macrosManager;
		this.globalScope = Scope.ofGlobalScope();
		this.parseResult = new ParseResult();
	}

	public PathsResolver getPathsResolver() {
		return pathsResolver;
	}

	public ParseOrderResolver getOrderResolver() {
		return orderResolver;
	}

	public MacrosManager getMacrosManager() {
		return macrosManager;
	}

	public Scope getGlobalScope() {
		return globalScope;
	}

	public ParseResult getParseResult() {
		return parseResult;
	}

}
