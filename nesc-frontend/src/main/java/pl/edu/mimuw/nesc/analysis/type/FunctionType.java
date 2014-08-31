package pl.edu.mimuw.nesc.analysis.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects a type of a function, e.g. <code>int(char, unsigned int)</code>.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FunctionType extends DerivedType {
    /**
     * Type of the value that is returned by a function of this function type.
     * Never null.
     */
    private final Type returnType;

    /**
     * Types of arguments that a function of this function type must be given.
     * Never null and all of the elements of the array also never null.
     */
    private final List<Type> argumentsTypes;

    /**
     * Initializes this function type.
     *
     * @throws NullPointerException One of the arguments is null.
     * @throws IllegalArgumentException One of the elements of the given list is
     *                                  null.
     */
    public FunctionType(Type returnType, List<Type> argumentsTypes) {
        super(false, false);

        // Validate the arguments
        checkNotNull(returnType, "return type cannot be null");
        checkNotNull(argumentsTypes, "arguments types cannot be null");
        for (Type type : argumentsTypes) {
            checkArgument(type != null, "a type of an argument of a function type cannot be null");
        }

        // Initialize this object
        this.returnType = returnType;
        this.argumentsTypes = Collections.unmodifiableList(new ArrayList<>(argumentsTypes));
    }

    @Override
    public final boolean isScalarType() {
        return false;
    }
}
