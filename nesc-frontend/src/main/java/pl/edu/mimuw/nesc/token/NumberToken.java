package pl.edu.mimuw.nesc.token;

import pl.edu.mimuw.nesc.ast.Location;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class NumberToken extends ConstantToken {

    public NumberToken(Location startLocation, Location endLocation, String value) {
        super(startLocation, endLocation, value);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

}
