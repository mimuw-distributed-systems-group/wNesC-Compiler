package pl.edu.mimuw.nesc.ast;

/**
 * Enumeration type that represents the semantics of a tag reference.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public enum StructSemantics {
    /**
     * The tag reference represents the definition but none of its fields have
     * been already parsed. The object does not contain information about fields
     * but contains about attributes.
     */
    PREDEFINITION,

    /**
     * The tag reference represents the definition that has been fully parsed
     * and processed and is contained in the object of type <code>TagRef</code>.
     */
    DEFINITION,

    /**
     * The tag reference represents either a declaration but not definition of
     * a tag or only a reference (that is neither a declaration nor
     * a definition).
     */
    OTHER,
}
