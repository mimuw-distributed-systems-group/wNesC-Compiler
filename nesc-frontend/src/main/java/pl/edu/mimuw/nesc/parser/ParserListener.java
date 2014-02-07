package pl.edu.mimuw.nesc.parser;

/**
 * Parser callbacks listener.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public interface ParserListener {

    /**
     * Called when all definitions prior to the configuration, component or
     * interface definition are parsed.
     */
    void extdefsFinished();

    /**
     * Called when global identifier or type was defined.
     *
     * @param id   identifier
     * @param type type
     */
    void globalId(String id, Integer type);

    /**
     * Called when currently being parsed component finds a dependency on
     * specified interface.
     *
     * @param currentEntityPath path of current entity
     * @param interfaceName     interface name
     */
    void interfaceDependency(String currentEntityPath, String interfaceName);

    /**
     * Called when currently being parsed component finds a dependency on
     * specified module or configuration.
     *
     * @param currentEntityPath path of current entity
     * @param componentName     name of component
     */
    void componentDependency(String currentEntityPath, String componentName);

}
