package pl.edu.mimuw.nesc.declaration.object.unique;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.type.CharType;
import pl.edu.mimuw.nesc.type.PointerType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.type.UnsignedIntType;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.Linkage;

/**
 * <p>Declaration object that represents the builtin constant NesC function
 * <code>uniqueCount</code>. It follows the singleton pattern and is intended
 * to allow easy recognition of usage of <code>uniqueCount</code>.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UniqueCountDeclaration extends FunctionDeclaration {
    /**
     * The only instance of this class.
     */
    private static final UniqueCountDeclaration instance;
    static {
        final Type[] argsTypes = { new PointerType(new CharType()) };
        final Type uniqueCountType = new pl.edu.mimuw.nesc.type.FunctionType(new UnsignedIntType(), argsTypes, false);

        instance = new Builder().interfaceName(null)
                .functionType(FunctionType.IMPLICIT)
                .instanceParameters(null)
                .uniqueName("uniqueCount")
                .type(uniqueCountType)
                .linkage(Linkage.EXTERNAL)
                .name("uniqueCount")
                .startLocation(Location.getDummyLocation())
                .build();
    }

    /**
     * <p>Get the only instance of this class.</p>
     *
     * @return The only instance of this class.
     */
    public static UniqueCountDeclaration getInstance() {
        return instance;
    }

    /**
     * Private constructor for singleton pattern.
     */
    private UniqueCountDeclaration(Builder builder) {
        super(builder);
        setDefined(true);
    }

    /**
     * Builder for <code>uniqueCount</code> declaration object.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class Builder extends FunctionDeclarationBuilder<UniqueCountDeclaration> {
        @Override
        protected UniqueCountDeclaration create() {
            return new UniqueCountDeclaration(this);
        }
    }
}
