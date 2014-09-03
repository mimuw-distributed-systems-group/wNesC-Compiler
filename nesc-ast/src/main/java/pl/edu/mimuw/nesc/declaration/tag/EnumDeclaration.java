package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.EnumRef;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.ast.type.EnumeratedType;
import pl.edu.mimuw.nesc.ast.type.Type;

import static com.google.common.base.Preconditions.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class EnumDeclaration extends TagDeclaration {
    /**
     * List with all enumerators from this enumeration type. It should be
     * unmodifiable. It may be empty (however, program that contains such
     * enumeration is ill-formed). This object is absent if and only if the
     * tag is not defined.
     */
    private final Optional<List<ConstantDeclaration>> enumerators;

    /**
     * Object that represents this enumeration from AST.
     */
    private final EnumRef astEnumRef;

    /**
     * Initializes this object in a way that it reflects a forward declaration
     * of an enumeration tag (nevertheless such declaration is forbidden in the
     * C standard).
     */
    public EnumDeclaration(String name, Location location, EnumRef astRef) {
        super(Optional.of(name), location, false);
        checkNotNull(astRef, "AST node cannot be null");
        this.enumerators = Optional.absent();
        this.astEnumRef = astRef;
    }

    /**
     * Initializes this object to reflect a definition of an enumeration tag.
     */
    public EnumDeclaration(Optional<String> name, Location location,
                           List<ConstantDeclaration> enumerators, EnumRef astRef) {
        super(name, location, true);

        checkNotNull(enumerators, "enumerators list cannot be null");
        checkNotNull(astRef, "AST node cannot be null");

        this.enumerators = Optional.of(Collections.unmodifiableList(new ArrayList<>(enumerators)));
        this.astEnumRef = astRef;
    }

    @Override
    public Type getType(boolean constQualified, boolean volatileQualified) {
        return new EnumeratedType(constQualified, volatileQualified, this);
    }

    @Override
    public <R, A> R visit(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
