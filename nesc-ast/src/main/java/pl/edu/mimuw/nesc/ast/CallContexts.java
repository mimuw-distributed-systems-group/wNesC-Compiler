package pl.edu.mimuw.nesc.ast;

public enum CallContexts {
	/**
	 * In atomic calls to this fn.
	 */
	CALL_ATOMIC,
	/**
	 * If non-atomic calls to this fn.
	 */
	CALL_NONATOMIC;
}
