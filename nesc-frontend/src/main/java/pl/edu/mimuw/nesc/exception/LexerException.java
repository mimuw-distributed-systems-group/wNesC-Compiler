package pl.edu.mimuw.nesc.exception;

public class LexerException extends CompilerException {

	private static final long serialVersionUID = -7406727066428884030L;

	public LexerException(String message) {
		super(message);
	}

	public LexerException(String message, Throwable cause) {
		super(message, cause);
	}

}
