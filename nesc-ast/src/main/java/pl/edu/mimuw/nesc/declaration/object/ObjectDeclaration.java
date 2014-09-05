package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.Declaration;

/**
 * <p>Object namespace is defined both in C and nesc standard.
 * Object namespace contains:
 * <ul>
 * <li>variables,</li>
 * <li>typedefs,</li>
 * <li>function names,</li>
 * <li>enumeration constants,</li>
 * <li>interface reference,</li>
 * <li>component reference,</li>
 * // TODO magic_string, magic_function
 * </ul>
 * etc.
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class ObjectDeclaration extends Declaration {

    protected final String name;

    /**
     * Type of the object that this declaration represents. It may not be
     * present, e.g. when the type given in the corresponding declaration has
     * been invalid. Artificial types are created for objects other than
     * variables and functions.
     */
    protected final Optional<Type> type;

    protected ObjectDeclaration(String name, Location location, Optional<Type> type) {
        super(location);
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Optional<Type> getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ObjectDeclaration other = (ObjectDeclaration) obj;
        return Objects.equal(this.name, other.name);
    }

    public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    public interface Visitor<R, A> {
        R visit(ComponentRefDeclaration componentRef, A arg);

        R visit(ConstantDeclaration constant, A arg);

        R visit(FunctionDeclaration function, A arg);

        R visit(InterfaceRefDeclaration interfaceRef, A arg);

        R visit(TypenameDeclaration typename, A arg);

        R visit(VariableDeclaration variable, A arg);
    }

}
