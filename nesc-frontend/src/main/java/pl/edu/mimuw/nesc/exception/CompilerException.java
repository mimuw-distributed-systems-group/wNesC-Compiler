package pl.edu.mimuw.nesc.exception;

public class CompilerException extends RuntimeException {

	private static final long serialVersionUID = 2530107080187334639L;

	public CompilerException(String message) {
		super(message);
	}

	public CompilerException(String message, Throwable cause) {
		super(message, cause);
	}

}
