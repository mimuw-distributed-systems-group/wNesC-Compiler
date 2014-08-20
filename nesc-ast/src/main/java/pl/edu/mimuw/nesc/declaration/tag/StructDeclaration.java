package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.StructRef;
import static com.google.common.base.Preconditions.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class StructDeclaration extends TagDeclaration {
    /**
     * AST node that represents this structure.
     */
    private final StructRef astStructRef;

    /**
     * Constructor for declarations of structure tags that are not definitions.
     */
    public StructDeclaration(Optional<String> name, Location location, StructRef astStructRef) {
        super(name, location, false);
        checkNotNull(astStructRef, "AST node of the structure cannot be null");
        this.astStructRef = astStructRef;
    }

    @Override
    public <R, A> R visit(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
