package pl.edu.mimuw.nesc.preprocessor;

import com.google.common.base.Optional;
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
    private final Optional<String> sourceFile;
	private final Macro processedObject;
    private final boolean isPrivate;

    public PreprocessorMacro(String name, Optional<String> sourceFile, Macro processedObject) {
        this(name, sourceFile, processedObject, false);
    }

	public PreprocessorMacro(String name, Optional<String> sourceFile, Macro processedObject, boolean isPrivate) {
		this.name = name;
        this.sourceFile = sourceFile;
		this.processedObject = processedObject;
        this.isPrivate = isPrivate;
	}

	public String getName() {
		return name;
	}

    public Optional<String> getSourceFile() {
        return sourceFile;
    }

    public Macro getProcessedObject() {
		return processedObject;
	}

    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
	public String toString() {
		return "{ PreprocessorMacro; {name=" + name + ", processedObject=" + processedObject + "}}";
	}

}
