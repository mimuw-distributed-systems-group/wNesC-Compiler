package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Optional;
import java.util.List;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.AttributeRef;
import pl.edu.mimuw.nesc.ast.type.Type;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class AttributeDeclaration extends FieldTagDeclaration<AttributeRef> {

    /**
     * Constructor for an attribute definition. Declarations of attributes that
     * are not definitions are forbidden.
     */
    public AttributeDeclaration(String name, Location location, AttributeRef astRef,
            List<FieldDeclaration> fields) {
        super(Optional.of(name), location, true, astRef, false, Optional.of(fields));
    }

    @Override
    public Type getType(boolean constQualified, boolean volatileQualified) {
        throw new UnsupportedOperationException("an attribute declaration " +
                 "does not support the operation of getting the type it represents");
    }

    @Override
    public <R, A> R visit(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
