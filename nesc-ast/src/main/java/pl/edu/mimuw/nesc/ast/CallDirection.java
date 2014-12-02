package pl.edu.mimuw.nesc.ast;

/**
 * <p>Class that represents the call direction when invoking NesC commands
 * and events. It is intended to specify edges in the directed graph of
 * specification entities of components.</p>
 *
 * <p>In case of bare commands and events the direction indicates the
 * implementation of them. In case of interfaces, the direction indicates
 * provided interfaces.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see pl.edu.mimuw.nesc.ast.gen.Connection Connection
 */
public enum CallDirection {
    /**
     * The direction is from {@link pl.edu.mimuw.nesc.ast.gen.Connection#endPoint1 endPoint1}
     * to {@link pl.edu.mimuw.nesc.ast.gen.Connection#endPoint2 endPoint2}.
     */
    FROM_1_TO_2,

    /**
     * The direction is from {@link pl.edu.mimuw.nesc.ast.gen.Connection#endPoint2 endPoint2}
     * to {@link pl.edu.mimuw.nesc.ast.gen.Connection#endPoint1 endPoint1}.
     */
    FROM_2_TO_1,
}
