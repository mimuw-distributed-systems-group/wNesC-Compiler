package pl.edu.mimuw.nesc.token;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Node;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class IdToken extends Token {

    protected final String id;
    protected Node astNode;

    protected IdToken(Location startLocation, Location endLocation, String id) {
        super(startLocation, endLocation);
        this.id = id;
    }

    public IdToken(Location startLocation, Location endLocation, Node astNode, String id) {
        super(startLocation, endLocation);
        this.id = id;
        this.astNode = astNode;
    }

    public String getId() {
        return id;
    }

    public Node getAstNode() {
        return astNode;
    }

    public void setAstNode(Node astNode) {
        this.astNode = astNode;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
