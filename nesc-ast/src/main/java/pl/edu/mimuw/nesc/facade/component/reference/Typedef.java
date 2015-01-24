package pl.edu.mimuw.nesc.facade.component.reference;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that represents a type definition from the specification of
 * a component.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Typedef {
    /**
     * Name of the type definition.
     */
    private final TypenameDeclaration declaration;

    /**
     * Type denoted by this type definition.
     */
    private final Optional<Type> type;

    Typedef(TypenameDeclaration declaration, Optional<Type> type) {
        checkNotNull(declaration, "declaration cannot be null");
        checkNotNull(type, "type cannot be null");

        this.declaration = declaration;
        this.type = type;
    }

    /**
     * Get the declaration object that represents the type definition. It is the
     * same instance that is in the symbol table.
     *
     * @return Declaration object associated with the type definition.
     */
    public TypenameDeclaration getDeclaration() {
        return declaration;
    }

    /**
     * Get the type denoted by this type definition.
     *
     * @return Type denoted by this type definition.
     */
    public Optional<Type> getType() {
        return type;
    }
}
