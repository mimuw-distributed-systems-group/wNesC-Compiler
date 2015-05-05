package pl.edu.mimuw.nesc.declaration;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.AttributeRef;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.StructRef;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.ast.gen.UnionRef;
import pl.edu.mimuw.nesc.declaration.label.LabelDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ComponentRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.declaration.object.VariableDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.AttributeDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.EnumDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.FieldDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.FieldTagDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.StructDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.UnionDeclaration;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class responsible for controlling the operation of copying declaration
 * objects for associating with nodes in instantiated components.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class CopyController {
    /**
     * Maps of declaration objects necessary for copying them.
     */
    private final Map<VariableDeclaration, VariableDeclaration> variables = new HashMap<>();
    private final Map<FunctionDeclaration, FunctionDeclaration> functions = new HashMap<>();
    private final Map<TypenameDeclaration, TypenameDeclaration> typenames = new HashMap<>();
    private final Map<LabelDeclaration, LabelDeclaration> labels = new HashMap<>();
    private final Map<FieldDeclaration, FieldDeclaration> fields = new HashMap<>();
    private final Map<ConstantDeclaration, ConstantDeclaration> constants = new HashMap<>();
    private final Map<StructDeclaration, StructDeclaration> structures = new HashMap<>();
    private final Map<UnionDeclaration, UnionDeclaration> unions = new HashMap<>();
    private final Map<EnumDeclaration, EnumDeclaration> enumerations = new HashMap<>();
    private final Map<AttributeDeclaration, AttributeDeclaration> attributes = new HashMap<>();
    private final Map<InterfaceRefDeclaration, InterfaceRefDeclaration> ifaceRefs = new HashMap<>();
    private final Map<ComponentRefDeclaration, ComponentRefDeclaration> componentRefs = new HashMap<>();

    /**
     * Map of nodes necessary to copy the declaration objects.
     */
    private final Map<Node, Node> nodesMap;

    /**
     * Map of unique names necessary to properly fill copied declaration
     * objects.
     */
    private final Map<String, String> uniqueNamesMap;

    /**
     * Function for obtaining the resulting types.
     */
    private final Function<Type, Type> typeFunction;

    /**
     * Functions for copying the declaration objects.
     */
    private final Function<LabelDeclaration, LabelDeclaration> factoryLabel = new Function<LabelDeclaration, LabelDeclaration>() {
        @Override
        public LabelDeclaration apply(LabelDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");
            return specimen.deepCopy(CopyController.this);
        }
    };
    private final Function<VariableDeclaration, VariableDeclaration> factoryVariable = new Function<VariableDeclaration, VariableDeclaration>() {
        @Override
        public VariableDeclaration apply(VariableDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");
            return specimen.deepCopy(CopyController.this);
        }
    };
    private final Function<TypenameDeclaration, TypenameDeclaration> factoryTypename = new Function<TypenameDeclaration, TypenameDeclaration>() {
        @Override
        public TypenameDeclaration apply(TypenameDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");
            return specimen.deepCopy(CopyController.this);
        }
    };
    private final Function<InterfaceRefDeclaration, InterfaceRefDeclaration> factoryInterfaceRef = new Function<InterfaceRefDeclaration, InterfaceRefDeclaration>() {
        @Override
        public InterfaceRefDeclaration apply(InterfaceRefDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");
            return specimen.deepCopy(CopyController.this);
        }
    };
    private final Function<FunctionDeclaration, FunctionDeclaration> factoryFunction = new Function<FunctionDeclaration, FunctionDeclaration>() {
        @Override
        public FunctionDeclaration apply(FunctionDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");
            return specimen.deepCopy(CopyController.this);
        }
    };
    private final Function<ConstantDeclaration, ConstantDeclaration> factoryConstant = new Function<ConstantDeclaration, ConstantDeclaration>() {
        @Override
        public ConstantDeclaration apply(ConstantDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");
            return specimen.deepCopy(CopyController.this);
        }
    };
    private final Function<ComponentRefDeclaration, ComponentRefDeclaration> factoryComponentRef = new Function<ComponentRefDeclaration, ComponentRefDeclaration>() {
        @Override
        public ComponentRefDeclaration apply(ComponentRefDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");
            return specimen.deepCopy(CopyController.this);
        }
    };

    /**
     * Safe copy maker for declaration objects of enumerated types.
     */
    private final SafeCopyMaker<EnumDeclaration> safeCopyMakerEnum = new SafeCopyMaker<EnumDeclaration>() {
        @Override
        public EnumDeclaration makeCopySafely(EnumDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");

            final EnumDeclaration copy = EnumDeclaration.declarationBuilder()
                    .astNode(mapNode(specimen.getAstNode()))
                    .name(specimen.getName().orNull(), mapUniqueName(specimen.getUniqueName()).orNull())
                    .startLocation(specimen.getLocation())
                    .build();

            if (specimen.hasLayout()) {
                copy.setLayout(specimen.getSize(), specimen.getAlignment());
            }

            if (specimen.hasCompatibleType()) {
                copy.setCompatibleType(specimen.getCompatibleType());
            }

            if (specimen.isCorrect().isPresent()) {
                copy.setIsCorrect(specimen.isCorrect().get());
            }

            if (specimen.isTransformed()) {
                copy.transformed();
            }

            return copy;
        }

        @Override
        public void correctCopy(EnumDeclaration copy, EnumDeclaration specimen) {
            checkNotNull(copy, "copy cannot be null");
            checkNotNull(specimen, "specimen cannot be null");

            if (specimen.isDefined()) {
                final List<ConstantDeclaration> constantsCopies =
                        new ArrayList<>(specimen.getConstants().get().size());

                for (ConstantDeclaration constant : specimen.getConstants().get()) {
                    constantsCopies.add(copy(constant));
                }

                copy.define(constantsCopies);
            }
        }
    };

    /**
     * Safe copy maker for declaration objects of field declarations.
     */
    private final SafeCopyMaker<FieldDeclaration> safeCopyMakerField = new SafeCopyMaker<FieldDeclaration>() {
        @Override
        public FieldDeclaration makeCopySafely(FieldDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");

            final FieldDeclaration copy = new FieldDeclaration(specimen.getName(),
                    specimen.getLocation(), specimen.getEndLocation(), specimen.isBitField(),
                    mapNode(specimen.getAstField()));

            if (specimen.hasLayout()) {
                copy.setLayout(specimen.getOffsetInBits(), specimen.getSizeInBits(),
                        specimen.getAlignmentInBits());
            }

            return copy;
        }

        @Override
        public void correctCopy(FieldDeclaration copy, FieldDeclaration specimen) {
            checkNotNull(copy, "copy cannot be null");
            checkNotNull(specimen, "specimen cannot be null");

            copy.setType(mapType(specimen.getType()));
        }
    };

    /**
     * Safe copy maker for a declaration object of a structure.
     */
    private final SafeCopyMaker<StructDeclaration> safeCopyMakerStruct = new FieldTagDeclarationSafeCopyMaker<StructRef, StructDeclaration>() {
        @Override
        public StructDeclaration makeCopySafely(StructDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");
            final StructDeclaration.Builder builder = specimen.isDefined()
                    ? StructDeclaration.preDefinitionBuilder()
                    : StructDeclaration.declarationBuilder();
            builder.isExternal(specimen.isExternal());
            return createBasicCopy(builder, specimen);
        }
    };

    /**
     * Safe copy maker for a declaration object of an union.
     */
    private final SafeCopyMaker<UnionDeclaration> safeCopyMakerUnion = new FieldTagDeclarationSafeCopyMaker<UnionRef, UnionDeclaration>() {
        @Override
        public UnionDeclaration makeCopySafely(UnionDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");
            final UnionDeclaration.Builder builder = specimen.isDefined()
                    ? UnionDeclaration.preDefinitionBuilder()
                    : UnionDeclaration.declarationBuilder();
            builder.isExternal(specimen.isExternal());
            return createBasicCopy(builder, specimen);
        }
    };

    /**
     * Safe copy maker for a declaration object of an attribute definition.
     */
    private final SafeCopyMaker<AttributeDeclaration> safeCopyMakerAttribute = new FieldTagDeclarationSafeCopyMaker<AttributeRef, AttributeDeclaration>() {
        @Override
        public AttributeDeclaration makeCopySafely(AttributeDeclaration specimen) {
            checkNotNull(specimen, "specimen cannot be null");
            return createBasicCopy(AttributeDeclaration.preDefinitionBuilder(), specimen);
        }
    };

    /**
     * Function that maps unique names from generic components to the remangled
     * ones.
     */
    private final Function<String, String> uniqueNameFunction = new Function<String, String>() {
        @Override
        public String apply(String uniqueName) {
            return mapUniqueName(uniqueName);
        }
    };

    /**
     * Visitor that copies visited object declaration.
     */
    private final ObjectDeclaration.Visitor<ObjectDeclaration, Void> objectDeclarationGateway =
            new ObjectDeclaration.Visitor<ObjectDeclaration, Void>() {

        @Override
        public ObjectDeclaration visit(ComponentRefDeclaration componentRef, Void arg) {
            return copy(componentRef);
        }

        @Override
        public ObjectDeclaration visit(ConstantDeclaration constant, Void arg) {
            return copy(constant);
        }

        @Override
        public ObjectDeclaration visit(FunctionDeclaration function, Void arg) {
            return copy(function);
        }

        @Override
        public ObjectDeclaration visit(InterfaceRefDeclaration interfaceRef, Void arg) {
            return copy(interfaceRef);
        }

        @Override
        public ObjectDeclaration visit(TypenameDeclaration typename, Void arg) {
            return copy(typename);
        }

        @Override
        public ObjectDeclaration visit(VariableDeclaration variable, Void arg) {
            return copy(variable);
        }
    };

    /**
     * Visitor for copying visited tag declarations.
     */
    private final TagDeclaration.Visitor<TagDeclaration, Void> tagDeclarationGateway =
            new TagDeclaration.Visitor<TagDeclaration, Void>() {

        @Override
        public TagDeclaration visit(AttributeDeclaration attribute, Void arg) {
            return copy(attribute);
        }

        @Override
        public TagDeclaration visit(EnumDeclaration enumDeclaration, Void arg) {
            return copy(enumDeclaration);
        }

        @Override
        public TagDeclaration visit(StructDeclaration struct, Void arg) {
            return copy(struct);
        }

        @Override
        public TagDeclaration visit(UnionDeclaration union, Void arg) {
            return copy(union);
        }
    };

    /**
     * Visitor that maps visited tag declarations.
     */
    private final TagDeclaration.Visitor<TagDeclaration, Void> tagDeclarationMapGateway =
            new TagDeclaration.Visitor<TagDeclaration, Void>() {

        @Override
        public TagDeclaration visit(AttributeDeclaration attribute, Void arg) {
            return map(attribute);
        }

        @Override
        public TagDeclaration visit(EnumDeclaration enumDeclaration, Void arg) {
            return map(enumDeclaration);
        }

        @Override
        public TagDeclaration visit(StructDeclaration struct, Void arg) {
            return map(struct);
        }

        @Override
        public TagDeclaration visit(UnionDeclaration union, Void arg) {
            return map(union);
        }
    };

    /**
     * Initialize this copy controller to use given maps while copying
     * declaration objects.
     *
     * @param nodesMap Map with AST nodes.
     * @param uniqueNamesMap Map with unique names.
     */
    public CopyController(Map<Node, Node> nodesMap, Map<String, String> uniqueNamesMap,
            Function<Type, Type> typeTransform) {
        checkNotNull(nodesMap, "nodes map cannot be null");
        checkNotNull(uniqueNamesMap, "map of unique names cannot be null");
        checkNotNull(typeTransform, "function for transforming types cannot be null");

        this.nodesMap = nodesMap;
        this.uniqueNamesMap = uniqueNamesMap;
        this.typeFunction = typeTransform;
    }

    public LabelDeclaration copy(LabelDeclaration declaration) {
        return copy(declaration, labels, factoryLabel);
    }

    public VariableDeclaration copy(VariableDeclaration declaration) {
        return copy(declaration, variables, factoryVariable);
    }

    public TypenameDeclaration copy(TypenameDeclaration declaration) {
        return copy(declaration, typenames, factoryTypename);
    }

    public InterfaceRefDeclaration copy(InterfaceRefDeclaration declaration) {
        return copy(declaration, ifaceRefs, factoryInterfaceRef);
    }

    public FunctionDeclaration copy(FunctionDeclaration declaration) {
        return copy(declaration, functions, factoryFunction);
    }

    public ConstantDeclaration copy(ConstantDeclaration declaration) {
        return copy(declaration, constants, factoryConstant);
    }

    public ComponentRefDeclaration copy(ComponentRefDeclaration declaration) {
        return copy(declaration, componentRefs, factoryComponentRef);
    }

    public FieldDeclaration copy(FieldDeclaration declaration) {
        return copySafely(declaration, fields, safeCopyMakerField);
    }

    public StructDeclaration copy(StructDeclaration declaration) {
        return copySafely(declaration, structures, safeCopyMakerStruct);
    }

    public UnionDeclaration copy(UnionDeclaration declaration) {
        return copySafely(declaration, unions, safeCopyMakerUnion);
    }

    public EnumDeclaration copy(EnumDeclaration declaration) {
        return copySafely(declaration, enumerations, safeCopyMakerEnum);
    }

    public AttributeDeclaration copy(AttributeDeclaration declaration) {
        return copySafely(declaration, attributes, safeCopyMakerAttribute);
    }

    public ObjectDeclaration copy(ObjectDeclaration declaration) {
        return declaration != null
                ? declaration.accept(objectDeclarationGateway, null)
                : null;
    }

    public TagDeclaration copy(TagDeclaration declaration) {
        return declaration != null
                ? declaration.accept(tagDeclarationGateway, null)
                : null;
    }

    public LabelDeclaration map(LabelDeclaration declaration) {
        return map(declaration, labels);
    }

    public VariableDeclaration map(VariableDeclaration declaration) {
        return map(declaration, variables);
    }

    public TypenameDeclaration map(TypenameDeclaration declaration) {
        return map(declaration, typenames);
    }

    public InterfaceRefDeclaration map(InterfaceRefDeclaration declaration) {
        return map(declaration, ifaceRefs);
    }

    public FunctionDeclaration map(FunctionDeclaration declaration) {
        return map(declaration, functions);
    }

    public ConstantDeclaration map(ConstantDeclaration declaration) {
        return map(declaration, constants);
    }

    public ComponentRefDeclaration map(ComponentRefDeclaration declaration) {
        return map(declaration, componentRefs);
    }

    public FieldDeclaration map(FieldDeclaration declaration) {
        return map(declaration, fields);
    }

    public StructDeclaration map(StructDeclaration declaration) {
        return map(declaration, structures);
    }

    public UnionDeclaration map(UnionDeclaration declaration) {
        return map(declaration, unions);
    }

    public EnumDeclaration map(EnumDeclaration declaration) {
        return map(declaration, enumerations);
    }

    public AttributeDeclaration map(AttributeDeclaration declaration) {
        return map(declaration, attributes);
    }

    public TagDeclaration map(TagDeclaration declaration) {
        return declaration != null
                ? declaration.accept(tagDeclarationMapGateway, null)
                : null;
    }

    /**
     * Get the mapping from the unique name in generic component to remangled
     * unique name. If the given unique name is not remangled,
     * IllegalArgumentException is thrown.
     *
     * @param uniqueName Unique name in the generic component.
     * @return Unique name in the instantiated component.
     */
    public String mapUniqueName(String uniqueName) {
        checkNotNull(uniqueName, "unique name cannot be null");
        final Optional<String> newUniqueName = Optional.fromNullable(uniqueNamesMap.get(uniqueName));
        checkArgument(newUniqueName.isPresent(), "cannot find the mapped unique name for '%s'", uniqueName);

        return newUniqueName.get();
    }

    /**
     * Acts exactly the same as {@link CopyController#mapUniqueName(String)}.
     *
     * @param oldUniqueName Unique name in the generic component.
     * @return Unique name in the instantiated component.
     */
    public Optional<String> mapUniqueName(Optional<String> oldUniqueName) {
        return oldUniqueName.transform(uniqueNameFunction);
    }

    /**
     * Get the mapping of the given type to the type to appear in the
     * instantiated component.
     *
     * @param type Type to be mapped.
     * @return Type to appear in the instantiated component.
     */
    public Optional<Type> mapType(Optional<Type> type) {
        return type.transform(typeFunction);
    }

    /**
     * Acts the same as {@link CopyController#mapType} but on a pure
     * <code>Type</code> object.
     */
    public Type mapType(Type type) {
        return typeFunction.apply(type);
    }

    public List<Optional<Type>> mapTypes(List<Optional<Type>> types) {
        final List<Optional<Type>> newTypes = new ArrayList<>();

        for (Optional<Type> type : types) {
            newTypes.add(mapType(type));
        }

        return newTypes;
    }

    public Optional<List<Optional<Type>>> mapTypes(Optional<? extends List<Optional<Type>>> types) {
        return types.isPresent()
                ? Optional.of(mapTypes(types.get()))
                : Optional.<List<Optional<Type>>>absent();
    }

    @SuppressWarnings("unchecked")
    public <T extends Node> T mapNode(T node) {
        final Optional<Node> result = Optional.fromNullable(nodesMap.get(node));
        checkState(result.isPresent(), "the given node is not contained in the map");
        checkState(node.getClass() == result.get().getClass(),
                "the node in the map has different class than the given one");

        return (T) result.get();
    }

    public void mapOwners() {
        for (Map.Entry<ConstantDeclaration, ConstantDeclaration> entry : constants.entrySet()) {
            entry.getValue().ownedBy(map(entry.getKey().getOwner()));
        }

        for (Map.Entry<FieldDeclaration, FieldDeclaration> entry : fields.entrySet()) {
            entry.getValue().ownedBy((FieldTagDeclaration<?>) map(entry.getKey().getOwner()));
        }
    }

    private <T> T copy(T toCopy, Map<T, T> map, Function<T, T> factoryFun) {
        if (toCopy == null) {
            return null;
        } else if (map.containsKey(toCopy)) {
            return map.get(toCopy);
        } else {
            final T copy = factoryFun.apply(toCopy);
            map.put(toCopy, copy);
            return copy;
        }
    }

    private <T> T copySafely(T toCopy, Map<T, T> map, SafeCopyMaker<T> safeCopyMaker) {
        if (toCopy == null) {
            return null;
        } else if (map.containsKey(toCopy)) {
            return map.get(toCopy);
        } else {
            final T copy = safeCopyMaker.makeCopySafely(toCopy);
            map.put(toCopy, copy);
            safeCopyMaker.correctCopy(copy, toCopy);
            return copy;
        }
    }

    private <T> T map(T key, Map<T, T> map) {
        checkNotNull(key, "key cannot be null");

        final Optional<T> value = Optional.fromNullable(map.get(key));
        checkState(value.isPresent(), "the given declaration object is not contained in the map");

        return value.get();
    }

    /**
     * Interface for making copies without falling into dangerous infinite copy
     * loops.
     *
     * @param <T> Type of copied objects.
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface SafeCopyMaker<T> {
        /**
         * Make copy of the given object without copying other elements with
         * methods of the controller or <code>deepCopy</code> methods.
         *
         * @param specimen Object to copy.
         * @return Newly created instance that is the copy of the given
         *         specimen. The copy does not need to be accurate.
         * @throws NullPointerException Given specimen is <code>null</code>.
         */
        T makeCopySafely(T specimen);

        /**
         * Make the object that is the first parameter to be an accurate copy
         * of the given specimen. Copies of elements using this copy controller
         * or <code>deepCopy</code> methods can be safely made.
         *
         * @param copy The object to be corrected.
         * @param specimen The desired state of the copy.
         * @throws NullPointerException <code>copy</code> or
         *                              <code>specimen</code> is
         *                              <code>null</code>/
         */
        void correctCopy(T copy, T specimen);
    }

    /**
     * Safe copy maker for declaration objects of field tags.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private abstract class FieldTagDeclarationSafeCopyMaker<D extends TagRef, T extends FieldTagDeclaration<D>>
                implements SafeCopyMaker<T> {
        @Override
        public void correctCopy(T copy, T specimen) {
            checkNotNull(copy, "copy cannot be null");
            checkNotNull(specimen, "specimen cannot be null");

            if (specimen.isDefined()) {
                copy.define(specimen.getStructure().get());
            }
        }

        protected T createBasicCopy(FieldTagDeclaration.Builder<D, T> builder, T specimen) {
            final T basicCopy = builder.astNode(mapNode(specimen.getAstNode()))
                    .name(specimen.getName().orNull(), mapUniqueName(specimen.getUniqueName()).orNull())
                    .startLocation(specimen.getLocation())
                    .build();

            if (specimen.hasLayout()) {
                basicCopy.setLayout(specimen.getSize(), specimen.getAlignment());
            }

            if (specimen.isCorrect().isPresent()) {
                basicCopy.setIsCorrect(specimen.isCorrect().get());
            }

            if (specimen.isTransformed()) {
                basicCopy.transformed();
            }

            return basicCopy;
        }
    }
}
