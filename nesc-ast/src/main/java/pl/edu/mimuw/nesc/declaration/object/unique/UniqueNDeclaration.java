package pl.edu.mimuw.nesc.declaration.object.unique;

import pl.edu.mimuw.nesc.ast.Location;
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
    private static final UniqueNDeclaration instance = new UniqueNDeclaration();

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
    private UniqueNDeclaration() {
        super(getConfiguredBuilder());
        setDefined(true);
    }

    private static Builder getConfiguredBuilder() {
        final Type[] argsTypes = { new PointerType(new CharType()), new UnsignedIntType() };
        final Type uniqueNType = new pl.edu.mimuw.nesc.type.FunctionType(new UnsignedIntType(), argsTypes, false);

        final Builder builder = builder();
        builder.interfaceName(null)
            .functionType(FunctionType.IMPLICIT)
            .instanceParameters(null)
            .uniqueName("uniqueN")
            .type(uniqueNType)
            .linkage(Linkage.EXTERNAL)
            .name("uniqueN")
            .startLocation(Location.getDummyLocation());

        return builder;
    }
}
