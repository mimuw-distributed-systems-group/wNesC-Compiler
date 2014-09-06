package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.type.TypeDefinitionType;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class TypenameDeclaration extends ObjectDeclaration {

    /**
     * Type that is defined by the type definition this object represents. It
     * shall be absent if the type is unknown.
     */
    private final Optional<Type> denotedType;

    public TypenameDeclaration(String name, Location location, Optional<Type> denotedType) {
        super(name, location, Optional.<Type>of(TypeDefinitionType.getInstance()));
        this.denotedType = denotedType;
    }

    public Optional<Type> getDenotedType() {
        return denotedType;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

}
