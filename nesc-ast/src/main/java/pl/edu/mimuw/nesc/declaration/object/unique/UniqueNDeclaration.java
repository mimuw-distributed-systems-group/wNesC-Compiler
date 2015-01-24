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
 * <p>Declaration that represents the builtin, constant NesC function
 * <code>uniqueN</code>. It follows the singleton pattern. It is created
 * to allow easy recognition of usages of this function.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UniqueNDeclaration extends FunctionDeclaration {
    /**
     * The only instance of this class.
     */
    private static final UniqueNDeclaration instance;
    static {
        final Type[] argsTypes = { new PointerType(new CharType()), new UnsignedIntType() };
        final Type uniqueNType = new pl.edu.mimuw.nesc.type.FunctionType(new UnsignedIntType(), argsTypes, false);

        instance = new Builder().interfaceName(null)
                .functionType(FunctionType.IMPLICIT)
                .instanceParameters((LinkedList<Declaration>) null)
                .uniqueName("uniqueN")
                .type(uniqueNType)
                .linkage(Linkage.EXTERNAL)
                .name("uniqueN")
                .startLocation(Location.getDummyLocation())
                .build();
    }

    /**
     * <p>Get the only instance of this class.</p>
     *
     * @return The only instance of this class.
     */
    public static UniqueNDeclaration getInstance() {
        return instance;
    }

    /**
     * Private constructor for the singleton pattern.
     */
    private UniqueNDeclaration(Builder builder) {
        super(builder);
        setDefined(true);
    }

    @Override
    public UniqueNDeclaration deepCopy(CopyController controller) {
        // this object is singleton
        return this;
    }

    /**
     * Builder for <code>uniqueN</code> declaration object.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class Builder extends FunctionDeclarationBuilder<UniqueNDeclaration> {
        @Override
        protected UniqueNDeclaration create() {
            return new UniqueNDeclaration(this);
        }
    }
}
