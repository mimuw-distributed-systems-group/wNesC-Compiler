package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class AttributeDeclaration extends TagDeclaration {

    public AttributeDeclaration(Optional<String> name, Location location) {
        super(name, location);
    }

    @Override
    public <R, A> R visit(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
