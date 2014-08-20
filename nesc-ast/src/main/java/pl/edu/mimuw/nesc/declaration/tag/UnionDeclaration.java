package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.UnionRef;

import static com.google.common.base.Preconditions.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class UnionDeclaration extends TagDeclaration {
    /**
     * AST node that corresponds to this declaration.
     */
    private final UnionRef astUnionRef;

    /**
     * Constructor for declarations of union tags that are not definitions.
     */
    public UnionDeclaration(Optional<String> name, Location location, UnionRef astUnionRef) {
        super(name, location, false);
        checkNotNull(astUnionRef, "AST node of a union declaration cannot be null");
        this.astUnionRef = astUnionRef;
    }

    @Override
    public <R, A> R visit(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
