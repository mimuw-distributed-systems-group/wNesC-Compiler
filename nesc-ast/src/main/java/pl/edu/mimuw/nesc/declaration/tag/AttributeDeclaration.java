package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Optional;
import java.util.List;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.AttributeRef;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.TreeElement;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class AttributeDeclaration extends FieldTagDeclaration<AttributeRef> {

    /**
     * Constructor for an object that represents the definition of an attribute
     * when its fields have not yet been parsed. It is stored in the symbol
     * table only during the operation of parsing its fields.
     */
    public AttributeDeclaration(String name, Location location, AttributeRef astRef) {
        super(Optional.of(name), location, StructKind.ATTRIBUTE, astRef,
              Optional.<List<TreeElement>>absent());
    }

    /**
     * Constructor for an attribute definition. Declarations of attributes that
     * are not definitions are forbidden.
     */
    public AttributeDeclaration(String name, Location location, AttributeRef astRef,
            List<TreeElement> structure) {
        super(Optional.of(name), location, StructKind.ATTRIBUTE, astRef,
              Optional.of(structure));
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
