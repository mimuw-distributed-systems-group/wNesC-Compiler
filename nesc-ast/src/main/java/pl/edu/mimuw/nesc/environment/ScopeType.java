package pl.edu.mimuw.nesc.environment;

/**
 * Scope types.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public enum ScopeType {

    /**
     * Global definitions.
     */
    GLOBAL(false),
    /**
     * Nested in the global scope and contains the parameters of generic
     * interface definitions.
     */
    INTERFACE_PARAMETER(true),
    /**
     * Nested in interface parameter scope and contains the interface's
     * commands and events.
     */
    INTERFACE(false),
    /**
     * Nested in the global scope and contains the parameters of generic
     * components definitions.
     */
    COMPONENT_PARAMETER(true),
    /**
     * Nested in component parameter scope and contains the component's
     * specification elements.
     */
    SPECIFICATION(false),
    /**
     * Nested in the specification scope. For modules, the implementation
     * holds the tasks, C declarations and definitions that form the module's
     * body.
     */
    MODULE_IMPLEMENTATION(false),
    /**
     * Nested in the specification scope. For configurations, the
     * implementation scope contains the names by which this component
     * refers to its included components.
     */
    CONFIGURATION_IMPLEMENTATION(false),
    /**
     * Nested in either global scope or in implementation scope and contains
     * function's parameters and interface parameters (in case of events and
     * commands of parameterised interfaces).
     */
    FUNCTION_PARAMETER(true),
    COMPOUND(false),
    OTHER(false);

    private final boolean isParameterScope;

    private ScopeType(boolean isParameterScope) {
        this.isParameterScope = isParameterScope;
    }

    /**
     * @return <code>true</code> if and only if this scope is a scope for
     *         parameters.
     */
    public boolean isParameterScope() {
        return isParameterScope;
    }
}
