package pl.edu.mimuw.nesc.declaration.object.unique;

import java.util.LinkedList;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.type.CharType;
import pl.edu.mimuw.nesc.type.PointerType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.type.UnsignedIntType;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.Linkage;

/**
 * <p>Declaration object that represents the builtin, constant NesC function
 * <code>unique</code>. It follows the singleton pattern to allow easy
 * recognition.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UniqueDeclaration extends FunctionDeclaration {
    /**
     * The only instance of this class.
     */
    private static final UniqueDeclaration instance;
    static {
        final Type[] argsTypes = { new PointerType(new CharType()) };
        final Type uniqueType = new pl.edu.mimuw.nesc.type.FunctionType(new UnsignedIntType(), argsTypes, false);
        instance = new Builder().interfaceName(null)
                .functionType(FunctionType.IMPLICIT)
                .instanceParameters((LinkedList<Declaration>) null)
                .uniqueName("unique")
                .type(uniqueType)
                .linkage(Linkage.EXTERNAL)
                .name("unique")
                .startLocation(Location.getDummyLocation())
                .build();
    }

    /**
     * <p>Get the only instance of this class.</p>
     *
     * @return The only instance of unique declaration.
     */
    public static UniqueDeclaration getInstance() {
        return instance;
    }

    /**
     * Private constructor for singleton pattern.
     */
    private UniqueDeclaration(Builder builder) {
        super(builder);
        setDefined(true);
    }

    @Override
    public UniqueDeclaration deepCopy(CopyController controller) {
        // this object is singleton and cannot be copied
        return this;
    }

    /**
     * Builder that creates declaration object for <code>unique</code>.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class Builder extends FunctionDeclarationBuilder<UniqueDeclaration> {
        @Override
        protected UniqueDeclaration create() {
            return new UniqueDeclaration(this);
        }
    }
}
