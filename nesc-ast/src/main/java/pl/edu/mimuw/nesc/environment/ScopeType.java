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
    GLOBAL,
    /**
     * Nested in the global scope and contains the parameters of generic
     * interface definitions.
     */
    INTERFACE_PARAMETER,
    /**
     * Nested in interface parameter scope and contains the interface's
     * commands nad events.
     */
    INTERFACE,
    /**
     * Nested in the global scope and contains the parameters of generic
     * components definitions.
     */
    COMPONENT_PARAMETER,
    /**
     * Nested in component parameter scope and contains the component's
     * specification elements.
     */
    SPECIFICATION,
    /**
     * Nested in the specification scope. For configurations, the
     * implementation scope contains the names by which this component
     * refers to its included components. For modules, the implementation
     * holds the tasks, C declarations and definitions that form the module's
     * body.
     */
    IMPLEMENTATION,
    /**
     * Nested in either global scope or in implementation scope and contains
     * function's parameters and interface parameters (in case of events and
     * commands of parameterised interfaces).
     */
    FUNCTION_PARAMETER,
    COMPOUND,
}
