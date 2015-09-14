package pl.edu.mimuw.nesc.ast;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public enum NescDeclarationKind {
    /**
     * A generic or ordinary interface.
     */
    INTERFACE,

    /**
     * A generic or ordinary module.
     */
    MODULE,

    /**
     * A generic or ordinary configuration.
     */
    CONFIGURATION,

    /**
     * A binary component.
     */
    BINARY_COMPONENT,
    ;
}
