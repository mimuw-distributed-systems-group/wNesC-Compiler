package pl.edu.mimuw.nesc.preprocessor;

import org.anarres.cpp.Macro;

/**
 * <p>
 * Preprocessor macro. Currently macro consists only of name and body.
 * </p>
 * <p>
 * TODO: Should be extended by methods for determining the kind of macro (e.g.
 * object-like, function-like, variadic, etc.).
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 *
 */
public class PreprocessorMacro {

	private final String name;
	private final Macro processedObject;

	public PreprocessorMacro(String name, Macro processedObject) {
		this.name = name;
		this.processedObject = processedObject;
	}

	public String getName() {
		return name;
	}

	public Macro getProcessedObject() {
		return processedObject;
	}

	@Override
	public String toString() {
		return "{ PreprocessorMacro; {name=" + name + ", processedObject=" + processedObject + "}}";
	}

}
