package pl.edu.mimuw.nesc.parser;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.edu.mimuw.nesc.PathsResolver;
import pl.edu.mimuw.nesc.exception.EntityNotFoundException;

/**
 * <p>
 * Keeps the order of processing of nesc source files, which involves the
 * preprocessing, lexical analysis and parsing phase.
 * </p>
 *
 * <p>
 * The first file to process is specified by client. Client also specifies the
 * directories where source files should be searched for. The source files are
 * searched for in given directories in the same order the directories appear in
 * search paths list.
 * </p>
 *
 * <p>
 * TODO: this class is supposed to build components graph in the future.
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 *
 */
public class ParseOrderResolver {

	private final String startFile;
	private final PathsResolver pathsResolver;
	/**
	 * Ordered list of file paths of definitions of entities.
	 */
	private final List<String> parseOrderList;
	/**
	 * Set of parsed entities names (not file paths).
	 */
	private final Set<String> parsedEntities;

	/**
	 * Creates ParseOrderResolver.
	 *
	 * @param startFile
	 *            the main configuration source file path
	 * @param pathsResolver
	 *            paths resolver
	 */
	public ParseOrderResolver(String startFile, PathsResolver pathsResolver) {
		checkNotNull(startFile, "start file path cannot be null");
		checkNotNull(pathsResolver, "paths resolver cannot be null");

		this.startFile = startFile;
		this.pathsResolver = pathsResolver;
		this.parseOrderList = new ArrayList<String>();
		this.parsedEntities = new HashSet<String>();

		this.parseOrderList.add(startFile);
		this.parsedEntities.add(startFile);
	}

	/**
	 * Returns start file path.
	 *
	 * @return start file path
	 */
	public String getStartFile() {
		return startFile;
	}

	/**
	 * Checks whether entity was already parsed.
	 *
	 * @param dependecyEntityName
	 *            entity name
	 * @return <code>true</code> when entity was already parsed,
	 *         <code>false</code> otherwise
	 */
	public boolean wasParsed(String dependecyEntityName) {
		checkNotNull(dependecyEntityName, "entity name cannot be null");
		return this.parsedEntities.contains(dependecyEntityName);
	}

	/**
	 * Returns path to file containing entity definition.
	 *
	 * @param dependecyEntityName
	 *            entity name.
	 * @return path to file
	 * @throws IllegalStateException
	 *             thrown when file containing entity definition was already
	 *             parsed
	 * @throws EntityNotFoundException
	 *             thrown when file containing entity definition was not found
	 */
	public String findEntity(String dependecyEntityName) throws EntityNotFoundException {
		checkNotNull(dependecyEntityName, "entity name cannot be null");
		checkState(!this.parsedEntities.contains(dependecyEntityName),
				"this method should be called when you ensure that entity was not parsed yet");

		final String entityPath = pathsResolver.getEntityFile(dependecyEntityName);
		if (entityPath == null) {
			// cannot find entity definition file
			throw new EntityNotFoundException("Cannot find enitity " + dependecyEntityName + ".");
		}
		// entity definition file found
		this.parsedEntities.add(dependecyEntityName);
		this.parseOrderList.add(entityPath);
		return entityPath;
	}

}
