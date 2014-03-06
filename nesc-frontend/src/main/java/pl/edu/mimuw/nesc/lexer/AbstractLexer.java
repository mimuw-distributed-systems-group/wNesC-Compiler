package pl.edu.mimuw.nesc.lexer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;

import com.google.common.base.Preconditions;

/**
 * Skeleton of lexer.
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public abstract class AbstractLexer implements Lexer {

	protected final String mainFilePath;
	protected final List<String> userIncludePaths;
	protected final List<String> systemIncludePaths;
	protected final List<String> includeFilePaths;
	protected final Collection<PreprocessorMacro> macros;
    protected final Map<String, String> unparsedMacros;

	protected final List<LexerListener> listeners;

	protected AbstractLexer(String mainFilePath, List<String> userIncludePaths, List<String> systemIncludePaths,
			List<String> includeFilePaths, Collection<PreprocessorMacro> macros, Map<String, String> unparsedMacros) {
		this.mainFilePath = mainFilePath;
		this.userIncludePaths = userIncludePaths;
		this.systemIncludePaths = systemIncludePaths;
		this.includeFilePaths = includeFilePaths;
		this.macros = macros;
        this.unparsedMacros = unparsedMacros;

		this.listeners = new ArrayList<>();
	}

	@Override
	public void addListener(LexerListener listener) {
		Preconditions.checkNotNull(listener, "listener cannot be null");
		this.listeners.add(listener);
	}

	@Override
	public boolean removeListener(LexerListener listener) {
		Preconditions.checkNotNull(listener, "listener cannot be null");
		return this.listeners.remove(listener);
	}

}
