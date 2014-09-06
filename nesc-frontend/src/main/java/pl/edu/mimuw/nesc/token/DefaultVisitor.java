package pl.edu.mimuw.nesc.token;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class DefaultVisitor<R, A> implements Token.Visitor<R, A> {

    @Override
    public R visit(MacroToken macro, A arg) { return null; }

}
