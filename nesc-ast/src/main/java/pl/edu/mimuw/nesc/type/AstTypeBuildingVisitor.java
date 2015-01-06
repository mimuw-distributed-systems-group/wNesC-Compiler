package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import java.util.LinkedList;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.common.util.list.Lists;

/**
 * <p>Visitor that is responsible for building representations of types as AST
 * nodes.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class AstTypeBuildingVisitor implements TypeVisitor<Void, Void> {
    /**
     * Variable that accumulates the result.
     */
    private final AstType accumulatedType = new AstType(
            Location.getDummyLocation(),
            Optional.<Declarator>absent(),
            Lists.<TypeElement>newList()
    );

    /**
     * Get the AST type built by the visitor.
     *
     * @return AST type built by this visitor after visiting a type.
     */
    public AstType getAstType() {
        return accumulatedType;
    }

    @Override
    public Void visit(ArrayType type, Void arg) {
        assignType(type);

        accumulatedType.setDeclarator(Optional.<Declarator>of(new ArrayDeclarator(
                Location.getDummyLocation(),
                accumulatedType.getDeclarator(),
                Optional.<Expression>absent()
        )));
        type.getElementType().accept(this, null);

        return null;
    }

    @Override
    public Void visit(CharType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.CHAR);
        return null;
    }

    @Override
    public Void visit(ComponentType type, Void arg) {
        throw new UnsupportedOperationException("building AST type for an artificial type");
    }

    @Override
    public Void visit(DoubleType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.DOUBLE);
        return null;
    }

    @Override
    public Void visit(EnumeratedType type, Void arg) {
        assignType(type);

        /* Long type is used instead of direct usage of 'enum' to deal with
           variables of anonymous enumerated types. It is correct to assign
           a value of an enumerated type to a variable of 'long' type and
           vice-versa. */
        addPrimitiveTypeElements(type, RID.LONG);

        return null;
    }

    @Override
    public Void visit(ExternalStructureType type, Void arg) {
        final NxStructRef tagRef = new NxStructRef(
                Location.getDummyLocation(),
                Lists.<Attribute>newList(),
                Lists.<Declaration>newList(),
                new Word(Location.getDummyLocation(), type.getDeclaration().getName().get()),
                StructSemantics.OTHER
        );
        tagRef.setDeclaration(type.getDeclaration());
        buildTagRefType(type, tagRef);
        return null;
    }

    @Override
    public Void visit(ExternalUnionType type, Void arg) {
        final NxUnionRef tagRef = new NxUnionRef(
                Location.getDummyLocation(),
                Lists.<Attribute>newList(),
                Lists.<Declaration>newList(),
                new Word(Location.getDummyLocation(), type.getDeclaration().getName().get()),
                StructSemantics.OTHER
        );
        tagRef.setDeclaration(type.getDeclaration());
        buildTagRefType(type, tagRef);
        return null;
    }

    @Override
    public Void visit(FloatType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.FLOAT);
        return null;
    }

    @Override
    public Void visit(FunctionType type, Void arg) {
        assignType(type);

        // Create parameters declarations
        final LinkedList<Declaration> paramsDeclarations = new LinkedList<>();
        for (Optional<Type> argType : type.getArgumentsTypes()) {
            if (!argType.isPresent()) {
                throw new RuntimeException("Building an AST type for a function type with absent argument type");
            }
            final AstType argAstType = argType.get().toAstType();
            paramsDeclarations.add(new DataDecl(
                    Location.getDummyLocation(),
                    argAstType.getQualifiers(),
                    Lists.<Declaration>newList(new VariableDecl(
                            Location.getDummyLocation(),
                            argAstType.getDeclarator(),
                            Lists.<Attribute>newList(),
                            Optional.<AsmStmt>absent()
                    ))
            ));
        }

        // Add the ellipsis if it is present
        if (type.getVariableArguments()) {
            paramsDeclarations.add(new EllipsisDecl(Location.getDummyLocation()));
        }

        accumulatedType.setDeclarator(Optional.<Declarator>of(new FunctionDeclarator(
                Location.getDummyLocation(),
                accumulatedType.getDeclarator(),
                paramsDeclarations,
                Optional.<LinkedList<Declaration>>absent(),
                Lists.<TypeElement>newList()
        )));
        type.getReturnType().accept(this, null);

        return null;
    }

    @Override
    public Void visit(InterfaceType type, Void arg) {
        throw new UnsupportedOperationException("building AST type for an artificial type");
    }

    @Override
    public Void visit(IntType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.INT);
        return null;
    }

    @Override
    public Void visit(LongDoubleType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.LONG, RID.DOUBLE);
        return null;
    }

    @Override
    public Void visit(LongLongType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.LONG, RID.LONG);
        return null;
    }

    @Override
    public Void visit(LongType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.LONG);
        return null;
    }

    @Override
    public Void visit(PointerType type, Void arg) {
        assignType(type);

        final LinkedList<TypeElement> qualifiers = makeTypeQualifiersList(type.isConstQualified(),
                type.isVolatileQualified(), type.isRestrictQualified());

        if (!qualifiers.isEmpty()) {
            accumulatedType.setDeclarator(Optional.<Declarator>of(new QualifiedDeclarator(
                    Location.getDummyLocation(),
                    accumulatedType.getDeclarator(),
                    qualifiers
            )));
        }

        accumulatedType.setDeclarator(Optional.<Declarator>of(new PointerDeclarator(
                Location.getDummyLocation(),
                accumulatedType.getDeclarator()
        )));

        type.getReferencedType().accept(this, null);
        return null;
    }

    @Override
    public Void visit(ShortType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.SHORT);
        return null;
    }

    @Override
    public Void visit(SignedCharType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.SIGNED, RID.CHAR);
        return null;
    }

    @Override
    public Void visit(StructureType type, Void arg) {
        final StructRef tagRef = new StructRef(
                Location.getDummyLocation(),
                Lists.<Attribute>newList(),
                Lists.<Declaration>newList(),
                new Word(Location.getDummyLocation(), type.getDeclaration().getName().get()),
                StructSemantics.OTHER
        );
        tagRef.setDeclaration(type.getDeclaration());
        buildTagRefType(type, tagRef);
        return null;
    }

    @Override
    public Void visit(TypeDefinitionType type, Void arg) {
        throw new UnsupportedOperationException("building AST type for an artificial type");
    }

    @Override
    public Void visit(UnionType type, Void arg) {
        final UnionRef tagRef = new UnionRef(
                Location.getDummyLocation(),
                Lists.<Attribute>newList(),
                Lists.<Declaration>newList(),
                new Word(Location.getDummyLocation(), type.getDeclaration().getName().get()),
                StructSemantics.OTHER
        );
        tagRef.setDeclaration(type.getDeclaration());
        buildTagRefType(type, tagRef);
        return null;
    }

    @Override
    public Void visit(UnsignedCharType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.UNSIGNED, RID.CHAR);
        return null;
    }

    @Override
    public Void visit(UnsignedIntType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.UNSIGNED);
        return null;
    }

    @Override
    public Void visit(UnsignedLongLongType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.UNSIGNED, RID.LONG, RID.LONG);
        return null;
    }

    @Override
    public Void visit(UnsignedLongType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.UNSIGNED, RID.LONG);
        return null;
    }

    @Override
    public Void visit(UnsignedShortType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.UNSIGNED, RID.SHORT);
        return null;
    }

    @Override
    public Void visit(VoidType type, Void arg) {
        assignType(type);
        addPrimitiveTypeElements(type, RID.VOID);
        return null;
    }

    @Override
    public Void visit(UnknownType type, Void arg) {
        buildUnknownType(type);
        return null;
    }

    @Override
    public Void visit(UnknownArithmeticType type, Void arg) {
        buildUnknownType(type);
        return null;
    }

    @Override
    public Void visit(UnknownIntegerType type, Void arg) {
        buildUnknownType(type);
        return null;
    }

    private void assignType(Type type) {
        if (!accumulatedType.getType().isPresent()) {
            accumulatedType.setType(Optional.of(type));
        }
    }

    private void addPrimitiveTypeElements(Type primitiveType, RID... typeKeywords) {
        addTypeQualifiers(primitiveType.isConstQualified(), primitiveType.isVolatileQualified(), false);
        addTypeKeywords(typeKeywords);
    }

    private void addTagTypeElements(Type tagType, TagRef tagReference) {
        addTypeQualifiers(tagType.isConstQualified(), tagType.isVolatileQualified(), false);
        addTypeElement(tagReference);
    }

    private void addTypeKeywords(RID... typeKeywords) {
        final LinkedList<TypeElement> typeElements = accumulatedType.getQualifiers();

        for (RID rid : typeKeywords) {
            typeElements.add(new Rid(
                    Location.getDummyLocation(),
                    rid
            ));
        }
    }

    private void addTypeQualifiers(boolean addConstQualifier, boolean addVolatileQualifier,
            boolean addRestrictQualifier) {
        accumulatedType.getQualifiers().addAll(makeTypeQualifiersList(addConstQualifier,
                addVolatileQualifier, addRestrictQualifier));
    }

    private void addTypeElement(TypeElement typeElement) {
        accumulatedType.getQualifiers().add(typeElement);
    }

    private LinkedList<TypeElement> makeTypeQualifiersList(boolean addConstQualifier,
            boolean addVolatileQualifier, boolean addRestrictQualifier) {

        final LinkedList<TypeElement> typeQualifiers = new LinkedList<>();
        final RID[] qualifiersToAdd = {
                addConstQualifier ? RID.CONST : null,
                addVolatileQualifier ? RID.VOLATILE : null,
                addRestrictQualifier ? RID.RESTRICT : null,
        };

        for (RID qualifierRid : qualifiersToAdd) {
            if (qualifierRid != null) {
                typeQualifiers.add(new Qualifier(
                        Location.getDummyLocation(),
                        qualifierRid
                ));
            }
        }

        return typeQualifiers;
    }

    private void buildUnknownType(UnknownType unknownType) {
        assignType(unknownType);
        addTypeQualifiers(unknownType.isConstQualified(), unknownType.isVolatileQualified(), false);

        final Typename typename = new Typename(
                Location.getDummyLocation(),
                unknownType.getName()
        );
        typename.setIsGenericReference(true);
        addTypeElement(typename);
    }

    private void buildTagRefType(FieldTagType<?> tagType, TagRef tagRef) {
        if (!tagType.getDeclaration().getName().isPresent()) {
            throw new RuntimeException("building an AST type for an anonymous tag type");
        }

        assignType(tagType);

        tagRef.setUniqueName(tagType.getDeclaration().getUniqueName());
        tagRef.setNestedInNescEntity(tagType.getDeclaration().getAstNode().getNestedInNescEntity());
        tagRef.setSemantics(StructSemantics.OTHER);

        addTagTypeElements(tagType, tagRef);
    }
}
