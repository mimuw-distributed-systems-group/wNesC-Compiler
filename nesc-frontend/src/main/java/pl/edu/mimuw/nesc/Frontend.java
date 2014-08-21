package pl.edu.mimuw.nesc;

import pl.edu.mimuw.nesc.exception.InvalidOptionsException;

/**
 * <p> Compiler frontend interface. Provides methods for processing source
 * files in several ways.</p>
 *
 * <h1>Context</h1>
 *
 * <p>Each file has to be processed in the context. Context specifies obligatory
 * options such as:
 *
 * <ul>
 * <li>paths where source files should be searched for,</li>
 * <li>(header) files included by default,</li>
 * <li>predefined macros.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Each method must be called with context reference object {@link ContextRef}
 * indicating which context should be used.
 * </p>
 *
 * <p>Each context has its own cache.</p>
 *
 * <h1>File processing</h1>
 *
 * <p>Files could be processed in two ways.</p>
 *
 * <h2>Rebuild</h2>
 *
 * <p>Rebuilds the entire project.</p>
 *
 * <h2>Update</h2>
 * <p>
 * Parses and analyzes only a specified file. To obtain definitions from other
 * files the frontend uses, when possible, cached data from previous frontend
 * actions. Therefore more than one file may be processed in some cases.
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @see ContextRef
 */
public interface Frontend {

    /**
     * <p>Creates a new context.</p>
     *
     * @param args context arguments
     * @return context reference
     * @throws InvalidOptionsException
     */
    ContextRef createContext(String[] args) throws InvalidOptionsException;

    /**
     * <p>Deletes context.</p>
     *
     * @param contextRef context reference
     */
    void deleteContext(ContextRef contextRef);

    /**
     * Rebuilds the entire project.
     *
     * @param contextRef context reference
     * @return result of analysis of the entire project
     */
    ProjectData rebuild(ContextRef contextRef);

    /**
     * <p>Parses and analyzes the specified file.</p>
     * <p>Cached data is also used to retrieve external definitions during
     * processing specified file to avoid unnecessary processing.</p>
     *
     * @param contextRef context reference
     * @param filePath   file path
     * @return an object representing parsed data of the entire project.
     * In particular, it contains the list of results of parsing the file,
     * the first element is the data of the file with specified path,
     * the following elements are datas of files that are dependencies of
     * the given file but were not parsed before
     */
    ProjectData update(ContextRef contextRef, String filePath);
}
