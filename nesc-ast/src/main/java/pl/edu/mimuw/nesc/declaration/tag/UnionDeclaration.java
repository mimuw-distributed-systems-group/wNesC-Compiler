package pl.edu.mimuw.nesc.declaration.tag;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.UnionRef;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.type.ExternalUnionType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.type.UnionType;

import java.util.List;


/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class UnionDeclaration extends FieldTagDeclaration<UnionRef> {
    /**
     * Constructor for declarations of union tags that are not definitions.
     */
    public UnionDeclaration(String name, Location location, UnionRef astUnionRef,
                            boolean isExternal) {
        super(Optional.of(name), location, false, astUnionRef, isExternal,
              Optional.<List<FieldDeclaration>>absent());
    }

    /**
     * Constructor for declarations that are also definitions.
     */
    public UnionDeclaration(Optional<String> name, Location location, UnionRef astUnionRef,
                            boolean isExternal, List<FieldDeclaration> fields) {
        super(name, location, true, astUnionRef, isExternal, Optional.of(fields));
    }

    @Override
    public Type getType(boolean constQualified, boolean volatileQualified) {
        return   isExternal()
               ? new ExternalUnionType(constQualified, volatileQualified, this)
               : new UnionType(constQualified, volatileQualified, this);
    }

    @Override
    public <R, A> R visit(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
