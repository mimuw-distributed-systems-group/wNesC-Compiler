package pl.edu.mimuw.nesc.declaration.object;

/**
 * <p>An enumeration type that represents the kind of an object declaration
 * instance in the symbol table. It is intended to allow easy checking the kind
 * without using the <code>instanceof</code> operator.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public enum ObjectKind {
    COMPONENT,
    CONSTANT,
    FUNCTION,
    INTERFACE,
    TYPENAME,
    VARIABLE,
}
