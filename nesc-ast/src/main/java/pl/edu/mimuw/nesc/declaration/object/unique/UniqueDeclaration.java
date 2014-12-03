package pl.edu.mimuw.nesc.declaration.object.unique;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.type.CharType;
import pl.edu.mimuw.nesc.ast.type.PointerType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.type.UnsignedIntType;
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
    private static final UniqueDeclaration instance = new UniqueDeclaration();

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
    private UniqueDeclaration() {
        super(getConfiguredBuilder());
        setDefined(true);
    }

    private static Builder getConfiguredBuilder() {
        final Type[] argsTypes = { new PointerType(new CharType()) };
        final Type uniqueType = new pl.edu.mimuw.nesc.ast.type.FunctionType(new UnsignedIntType(), argsTypes, false);

        final Builder builder = builder();
        builder.interfaceName(null)
            .functionType(FunctionType.IMPLICIT)
            .instanceParameters(null)
            .uniqueName("unique")
            .type(uniqueType)
            .linkage(Linkage.EXTERNAL)
            .name("unique")
            .startLocation(Location.getDummyLocation());

        return builder;
    }
}
