package pl.edu.mimuw.nesc.declaration.tag;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.StructRef;
import pl.edu.mimuw.nesc.ast.type.ExternalStructureType;
import pl.edu.mimuw.nesc.ast.type.StructureType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.TreeElement;

import com.google.common.base.Optional;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class StructDeclaration extends FieldTagDeclaration<StructRef> {
    /**
     * Constructor for declarations of structure tags that are not definitions.
     */
    public StructDeclaration(String name, Location location, StructRef astStructRef,
                             boolean isExternal) {
        super(Optional.of(name), location, determineKind(isExternal), astStructRef,
              Optional.<List<TreeElement>>absent());
    }

    /**
     * Constructor for definitions of structure tags.
     *
     * @throws NullPointerException <code>astStructRef</code> or <code>fields</code>
     *                              is null.
     */
    public StructDeclaration(Optional<String> name, Location location, StructRef astStructRef,
                             boolean isExternal, List<TreeElement> structure) {
        super(name, location, determineKind(isExternal), astStructRef,
              Optional.of(structure));
    }

    @Override
    public Type getType(boolean constQualified, boolean volatileQualified) {
        return   isExternal()
               ? new ExternalStructureType(constQualified, volatileQualified, this)
               : new StructureType(constQualified, volatileQualified, this);
    }

    private static StructKind determineKind(boolean isExternal) {
        return   isExternal
               ? StructKind.NX_STRUCT
               : StructKind.STRUCT;
    }

    @Override
    public <R, A> R visit(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
