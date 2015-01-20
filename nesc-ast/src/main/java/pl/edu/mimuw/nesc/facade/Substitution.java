package pl.edu.mimuw.nesc.facade;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pl.edu.mimuw.nesc.type.*;

import static com.google.common.base.Preconditions.*;

/**
 * A class that represents a substitution. Unknown types are substituted by
 * actual types.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class Substitution {
    /**
     * Map that defines the substitution. Keys are names of unknown types.
     */
    private final ImmutableMap<String, Optional<Type>> substitution;

    /**
     * Instance of substitute visitor that will be used for performing
     * substitutions.
     */
    private final TypeSubstituteVisitor substituteVisitor = new TypeSubstituteVisitor();

    /**
     * Function that substitutes all all types from given list.
     */
    private final Function<ImmutableList<Optional<Type>>, ImmutableList<Optional<Type>>> SUBSTITUTE_TRANSFORM =
            new Function<ImmutableList<Optional<Type>>, ImmutableList<Optional<Type>>>() {
        @Override
        public ImmutableList<Optional<Type>> apply(ImmutableList<Optional<Type>> types) {
            checkNotNull(types, "list of types cannot be null");
            return substituteVisitor.doSubstitution(types);
        }
    };

    /**
     * Get a builder that will create a substitution.
     *
     * @return Newly created builder that will build a substitution.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initialize this object with data from the given builder.
     *
     * @param builder Builder with necessary information.
     */
    private Substitution(Builder builder) {
        this.substitution = builder.buildSubstitution();
    }

    /**
     * Acts identically as {@link Substitution#substituteType(Type)} but
     * performs the substitution only if the type is present.
     *
     * @param type Type whose unknown types will be substituted.
     * @return Type that is the result of performing the substitution.
     * @throws NullPointerException Type is null.
     */
    public Optional<Type> substituteType(Optional<Type> type) {
        checkNotNull(type, "type cannot be null");
        return type.isPresent()
                ? substituteType(type.get())
                : type;
    }

    /**
     * <p>Performs the substitution on the given type. Unknown types that are
     * elements of the given type will be substituted and the resulting type
     * will be returned. If this substitution does not contain a mapping from
     * an unknown type in the given type, then the returned object will be
     * absent.</p>
     *
     * @param type Type that will have unknown types substituted.
     * @return A type that is the result of performing the substitution.
     * @throws NullPointerException Type is null.
     */
    public Optional<Type> substituteType(Type type) {
        checkNotNull(type, "type cannot be null");
        return type.accept(substituteVisitor, null);
    }

    /**
     * <p>Performs the substitution for all types in the given list. The
     * returned list has the same length as the given list.</p>
     *
     * @param types List of types to perform the substitution.
     * @return Immutable list of types that is result of performing
     *         substitution on given list.
     * @throws NullPointerException List is null.
     */
    public ImmutableList<Optional<Type>> substituteTypes(ImmutableList<Optional<Type>> types) {
        checkNotNull(types, "list of types cannot be null");
        return substituteVisitor.doSubstitution(types);
    }

    /**
     * <p>Performs substitution on all types on the given list if it is present
     * and returns the result of the substitution.</p>
     *
     * @param types List with types to substitute.
     * @return Result of the substitution.
     * @throws NullPointerException The list is null.
     */
    public Optional<ImmutableList<Optional<Type>>> substituteTypes(Optional<ImmutableList<Optional<Type>>> types) {
        checkNotNull(types, "list of types cannot be null");
        return types.transform(SUBSTITUTE_TRANSFORM);
    }

    /**
     * Visitor that is responsible for substituting unknown types that occur
     * in a given type. It performs an action that is similar to cloning and
     * then applying some small changes. The substitution is defined by
     * {@link Substitution#substitution substitution}.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class TypeSubstituteVisitor implements TypeVisitor<Optional<Type>, Void> {

        // Primitive types

        @Override
        public Optional<Type> visit(CharType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(SignedCharType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnsignedCharType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(ShortType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnsignedShortType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(IntType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnsignedIntType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(LongType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnsignedLongType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(LongLongType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnsignedLongLongType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(EnumeratedType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(FloatType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(DoubleType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(LongDoubleType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(VoidType type, Void arg) {
            return Optional.<Type>of(type);
        }

        // Derived types

        @Override
        public Optional<Type> visit(final ArrayType oldType, Void arg) {
            final Function<Type, Type> arrayTypeTransform = new Function<Type, Type>() {
                @Override
                public Type apply(Type newElementType) {
                    return newElementType != oldType.getElementType()
                            ? new ArrayType(newElementType, oldType.getSize())
                            : oldType;
                }
            };

            return oldType.getElementType().accept(this, null)
                    .transform(arrayTypeTransform);
        }

        @Override
        public Optional<Type> visit(final PointerType oldType, Void arg) {
            final Function<Type, Type> pointerTypeTransform = new Function<Type, Type>() {
                @Override
                public Type apply(Type newReferencedType) {
                    return newReferencedType != oldType.getReferencedType()
                            ? new PointerType(oldType.isConstQualified(), oldType.isVolatileQualified(),
                            oldType.isRestrictQualified(), newReferencedType)
                            : oldType;
                }
            };

            return oldType.getReferencedType().accept(this, null)
                    .transform(pointerTypeTransform);
        }

        @Override
        public Optional<Type> visit(StructureType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnionType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(ExternalStructureType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(ExternalUnionType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(FunctionType type, Void arg) {
            boolean change;

            // Process return type
            final Optional<Type> returnType = type.getReturnType().accept(this, null);
            if (!returnType.isPresent()) {
                return returnType;
            }
            change = returnType.get() != type.getReturnType();

            // Process arguments types
            final ImmutableList<Optional<Type>> argsTypes = doSubstitution(type.getArgumentsTypes());
            change = change || argsTypes != type.getArgumentsTypes();

            final Type result = change
                    ? new FunctionType(returnType.get(), argsTypes, type.getVariableArguments())
                    : type;

            return Optional.of(result);
        }

        // Artificial types

        @Override
        public Optional<Type> visit(InterfaceType type, Void arg) {
            if (!type.getTypeParameters().isPresent()) {
                return Optional.<Type>of(type);
            }

            final ImmutableList<Optional<Type>> types =
                    doSubstitution(type.getTypeParameters().get());

            final InterfaceType result = types == type.getTypeParameters().get()
                    ? type
                    : new InterfaceType(type.getInterfaceName(), Optional.of(types));

            return Optional.<Type>of(result);
        }

        @Override
        public Optional<Type> visit(ComponentType type, Void arg) {
            throw new RuntimeException("unexpected artificial type");
        }

        @Override
        public Optional<Type> visit(TypeDefinitionType type, Void arg) {
            throw new RuntimeException("unexpected artificial type");
        }

        // Unknown types

        @Override
        public Optional<Type> visit(UnknownType type, Void arg) {
            return doSubstitution(type);
        }

        @Override
        public Optional<Type> visit(UnknownArithmeticType type, Void arg) {
            return doSubstitution(type);
        }

        @Override
        public Optional<Type> visit(UnknownIntegerType type, Void arg) {
            return doSubstitution(type);
        }

        private Optional<Type> doSubstitution(final UnknownType type) {
            final Function<Type, Type> enrichTypeTransform = new Function<Type, Type>() {
                @Override
                public Type apply(Type newType) {
                    return newType.addQualifiers(type);
                }
            };

            return Optional.fromNullable(substitution.get(type.getName()))
                    .or(Optional.<Type>absent())
                    .transform(enrichTypeTransform);
        }

        private ImmutableList<Optional<Type>> doSubstitution(ImmutableList<Optional<Type>> typesList) {
            final ImmutableList.Builder<Optional<Type>> builder = ImmutableList.builder();
            boolean change = false;

            for (Optional<Type> type : typesList) {
                if (type.isPresent()) {
                    final Optional<Type> newArgType = type.get().accept(this, null);
                    builder.add(newArgType);
                    change = change || !newArgType.isPresent() || newArgType.get() != type.get();
                } else {
                    builder.add(type);
                }
            }

            return change
                    ? builder.build()
                    : typesList;
        }
    }

    /**
     * Builder for a substitution.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder {
        /**
         * Data necessary to build a substitution.
         */
        private final List<String> names = new ArrayList<>();
        private final List<Optional<Type>> substitutedTypes = new ArrayList<>();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Adds next mapping in the substitution: a unknown type with given name
         * is substituted to the given type.
         *
         * @param name Name of the substituted unknown type.
         * @param substituteType Type that will be substituted for the given
         *                       name.
         * @return <code>this</code>
         */
        public Builder addMapping(String name, Optional<Type> substituteType) {
            this.names.add(name);
            this.substitutedTypes.add(substituteType);
            return this;
        }

        private void validate() {
            checkState(names.size() == substitutedTypes.size(),
                    "count of names of unknown types is different from the count of types (%d:%d)",
                    names.size(), substitutedTypes.size());

            final Set<String> usedNames = new HashSet<>();

            /* Check if parameters names are unique (relevant during
               construction of the substitution) */
            for (String paramName : names) {
                checkState(!usedNames.contains(paramName), "name '%s' of unknown type added multiple times",
                        paramName);
                usedNames.add(paramName);
            }
        }

        public Substitution build() {
            validate();
            return new Substitution(this);
        }

        private ImmutableMap<String, Optional<Type>> buildSubstitution() {
            final ImmutableMap.Builder<String, Optional<Type>> substBuilder = ImmutableMap.builder();

            for (int i = 0; i < names.size(); ++i) {
                substBuilder.put(names.get(i), substitutedTypes.get(i));
            }

            return substBuilder.build();
        }
    }
}
