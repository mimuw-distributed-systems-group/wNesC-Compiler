package pl.edu.mimuw.nesc.analysis;

/**
 * <p>Interface that specifies some events from the semantic analysis. It is
 * intended to provide callbacks from the analysis code to the parser.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface SemanticListener {
    /**
     * <p>Called when a name that will be located in the global scope is
     * detected. These are the names declared in the global scope (excluding
     * component and interface names) and declarations with @C() attribute in
     * the implementation of a non-generic module.</p>
     *
     * @param uniqueName String with the unique (mangled) name that has been
     *                   associated with the object.
     * @param name String with the global name that is detected. It is the name
     *             from the code not the unique name.
     */
    void globalName(String uniqueName, String name);

    /**
     * <p>Called when name mangling of the given name is required.</p>
     *
     * @param unmangledName Name that requires mangling.
     * @return String with the given name after mangling.
     */
    String nameManglingRequired(String unmangledName);

    /**
     * <p>Called when a type definition that contains information about
     * associated combining function is detected.</p>
     *
     * @param typedefUniqueName Unique name of the type definition.
     * @param functionName Name of the combining function associated with the
     *                     type definition with name from the first parameter.
     */
    void combiningFunction(String typedefUniqueName, String functionName);
}
