package pl.edu.mimuw.nesc.parser;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.common.NesCFileType;

/**
 * Parser callbacks listener.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public interface ParserListener {

    /**
     * Returns the type of recognized NesC entity. Is not called for
     * header files or C files.
     * @param nesCFileType file type
     */
    void nescEntityRecognized(NesCFileType nesCFileType);

    /**
     * Called when all definitions prior to the configuration, component or
     * interface definition are parsed.
     */
    void extdefsFinished();

    /**
     * Called when currently being parsed component finds a dependency on
     * specified interface.
     *
     * @param currentEntityPath path of current entity
     * @param interfaceName     interface name
     * @param visibleFrom       location of interface reference in source file
     * @return <code>true</code> if interface declaration was found,
     * <code>false</code> otherwise
     */
    boolean interfaceDependency(String currentEntityPath, String interfaceName, Location visibleFrom);

    /**
     * Called when currently being parsed component finds a dependency on
     * specified module or configuration.
     *
     * @param currentEntityPath path of current entity
     * @param componentName     name of component
     * @param visibleFrom       location of component reference in source file
     * @return <code>true</code> if component declaration was found,
     * <code>false</code> otherwise
     */
    boolean componentDependency(String currentEntityPath, String componentName, Location visibleFrom);

}
