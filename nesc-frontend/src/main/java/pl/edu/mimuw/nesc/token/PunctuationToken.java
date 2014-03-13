package pl.edu.mimuw.nesc.token;

import pl.edu.mimuw.nesc.ast.Location;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class PunctuationToken extends Token {

    public PunctuationToken(Location startLocation, Location endLocation) {
        super(startLocation, endLocation);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
