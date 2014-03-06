package pl.edu.mimuw.nesc;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

import pl.edu.mimuw.nesc.option.OptionsHolder;
import pl.edu.mimuw.nesc.option.OptionsParser;
import pl.edu.mimuw.nesc.parser.ParseOrderResolver;
import pl.edu.mimuw.nesc.preprocessor.MacrosManager;

/**
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 *
 */
public class Main {

	public static void main(String[] args) {
		run(args);
	}

	public static ParseResult run(String[] args) {
		/*
		 * Parse options.
		 */
		OptionsParser optionsParser = null;
		try {
			optionsParser = new OptionsParser();
			optionsParser.parse(args);
		} catch (ParseException e) {
			final String message = e.getMessage();
			System.out.println(message);
			optionsParser.printHelp();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		/*
		 * Initialize important classes.
		 */
		final PathsResolver pathsResolver = PathsResolver.builder()
				.sourcePaths(OptionsHolder.instance().getSourcePaths())
                .quoteIncludePaths(OptionsHolder.instance().getUserSourcePaths())
				.includeFilePaths(OptionsHolder.instance().getDefaultIncludeFiles())
				.projectPath(OptionsHolder.instance().getProjectPath())
				.build();

		// TODO: handle exception when start file was not found
		final String startFile = pathsResolver.getEntityFile(OptionsHolder.instance().getEntryEntity());
		final ParseOrderResolver orderResolver = new ParseOrderResolver(startFile, pathsResolver);

        /* Create macros manager with predefined macros. */
		final MacrosManager macrosManager = new MacrosManager();
        macrosManager.addUnparsedMacros(OptionsHolder.instance().getPredefinedMacros());

		final ParseContext context = new ParseContext(pathsResolver, orderResolver, macrosManager);
		final ParseExecutor parseExecutor = new ParseExecutor(context);
		parseExecutor.start();
		return context.getParseResult();
	}

}
