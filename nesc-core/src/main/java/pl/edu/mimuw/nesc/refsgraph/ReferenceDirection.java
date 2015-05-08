package pl.edu.mimuw.nesc.refsgraph;

/**
 * Enum type for the direction of the reference, e.g. whether an ordinary
 * identifier references an ordinary identifier.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public enum ReferenceDirection {
    FROM_ORDINARY_ID_TO_ORDINARY_ID,
    FROM_ORDINARY_ID_TO_TAG,
    FROM_TAG_TO_TAG,
    FROM_TAG_TO_ORDINARY_ID,
}
