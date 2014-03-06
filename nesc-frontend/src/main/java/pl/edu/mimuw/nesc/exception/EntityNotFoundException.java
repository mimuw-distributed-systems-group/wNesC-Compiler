package pl.edu.mimuw.nesc.exception;

public class EntityNotFoundException extends CompilerException {

	private static final long serialVersionUID = 1681609710588408677L;

	public EntityNotFoundException(String message) {
		super(message);
	}

}
