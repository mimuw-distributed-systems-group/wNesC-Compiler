package pl.edu.mimuw.nesc.instantiation;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.type.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Visitor responsible for ensuring that types that are associated with
 * expressions and 'AstType' objects have necessary information for computing
 * their size and alignment or are one of unnamed unknown type instances. In the
 * latter case, these expressions need further analysis to determine their
 * exact type.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see pl.edu.mimuw.nesc.type.UnknownType#unnamed
 * @see pl.edu.mimuw.nesc.type.UnknownIntegerType#unnamed
 * @see pl.edu.mimuw.nesc.type.UnknownArithmeticType#unnamed
 */
class ASTFillingVisitor extends IdentityVisitor<Void> implements TypeVisitor<Type, Void> {
    /**
     * Map with actual types associated with their names.
     */
    private final ImmutableMap<String, Type> typesMap;

    /**
     * Map from nodes in the generic component to corresponding nodes in the
     * instantiated component.
     */
    private final Map<Node, Node> nodesMap;

    /**
     * Object that is responsible for managing the copied objects.
     */
    private final CopyController copyController;

    /**
     * Initializes this visitor to substitute types using the mapping
     * defined by given component reference and generic component nodes.
     * AST nodes in types are substituted using given substitution manager.
     */
    public ASTFillingVisitor(ComponentRef componentRef,
            Component genericComponent, SubstitutionManager astSubstitution,
            Map<Node, Node> nodesMap, Map<String, String> uniqueNamesMap) {
        checkNotNull(componentRef, "component reference cannot be null");
        checkNotNull(genericComponent, "generic component cannot be null");
        checkNotNull(astSubstitution, "substitution manager cannot be null");
        checkNotNull(nodesMap, "nodes map cannot be null");
        checkNotNull(uniqueNamesMap, "unique names map cannot be null");

        final Function<Type, Type> typeFunction = new Function<Type, Type>() {
            @Override
            public Type apply(Type type) {
                checkNotNull(type, "type cannot be null");
                return type.accept(ASTFillingVisitor.this, null);
            }
        };

        final PrivateBuilder builder = new PrivateBuilder(componentRef, genericComponent);
        this.typesMap = builder.buildTypesMap();
        this.nodesMap = nodesMap;
        this.copyController = new CopyController(nodesMap, uniqueNamesMap, typeFunction);
    }

    /**
     * Make necessary changes in the given component type reference object. The
     * object shall refer an entity from the component that this visitor has
     * been originally created for.
     *
     * @param typename AST node that refers an entity from the filled component.
     */
    public void mapComponentTyperef(ComponentTyperef typename) {
        checkNotNull(typename, "component type reference cannot be null");
        typename.setDeclaration(copyController.map(typename.getDeclaration()));
        typename.setUniqueName(copyController.mapUniqueName(typename.getUniqueName()));
    }

    /**
     * Make changes necessary for the given component dereference expression.
     * The object shall refer an entity from the component this visitor has been
     * originally created for.
     *
     * @param expr Component dereference expression.
     */
    public void mapComponentDeref(ComponentDeref expr) {
        checkNotNull(expr, "component dereference cannot be null");

        final Identifier identifier = (Identifier) expr.getArgument();
        identifier.setDeclaration(copyController.map((ConstantDeclaration) identifier.getDeclaration()));
        identifier.setUniqueName(copyController.mapUniqueName(identifier.getUniqueName()));
    }

    @Override
    public Void visitEnumerator(Enumerator enumerator, Void arg) {
        enumerator.setDeclaration(copyController.copy(enumerator.getDeclaration()));
        return arg;
    }

    @Override
    public Void visitOldIdentifierDecl(OldIdentifierDecl declaration, Void arg) {
        declaration.setDeclaration(copyController.copy(declaration.getDeclaration()));
        return null;
    }

    @Override
    public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
        declaration.setDeclaration(copyController.copy(declaration.getDeclaration()));
        return null;
    }

    @Override
    public Void visitVariableDecl(VariableDecl declaration, Void arg) {
        declaration.setDeclaration(copyController.copy(declaration.getDeclaration()));
        return null;
    }

    @Override
    public Void visitFieldDecl(FieldDecl declaration, Void arg) {
        declaration.setDeclaration(copyController.copy(declaration.getDeclaration()));
        return null;
    }

    @Override
    public Void visitTypename(Typename typeElement, Void arg) {
        if (!VariousUtils.getBooleanValue(typeElement.getIsPasted()) && typeElement.getIsDeclaredInThisNescEntity()) {
            typeElement.setDeclaration(copyController.copy(typeElement.getDeclaration()));
        }
        return null;
    }

    @Override
    public Void visitStructRef(StructRef structRef, Void arg) {
        mapStructDeclaration(structRef);
        return null;
    }

    @Override
    public Void visitNxStructRef(NxStructRef nxStructRef, Void arg) {
        mapStructDeclaration(nxStructRef);
        return null;
    }

    private void mapStructDeclaration(StructRef structRef) {
        if (canCopyTagDeclaration(structRef)) {
            structRef.setDeclaration(copyController.copy(structRef.getDeclaration()));
        }
    }

    @Override
    public Void visitUnionRef(UnionRef unionRef, Void arg) {
        mapUnionDeclaration(unionRef);
        return null;
    }

    @Override
    public Void visitNxUnionRef(NxUnionRef nxUnionRef, Void arg) {
        mapUnionDeclaration(nxUnionRef);
        return null;
    }

    private void mapUnionDeclaration(UnionRef unionRef) {
        if (canCopyTagDeclaration(unionRef)) {
            unionRef.setDeclaration(copyController.copy(unionRef.getDeclaration()));
        }
    }

    @Override
    public Void visitAttributeRef(AttributeRef attributeRef, Void arg) {
        attributeRef.setDeclaration(copyController.copy(attributeRef.getDeclaration()));
        return null;
    }

    @Override
    public Void visitEnumRef(EnumRef enumRef, Void arg) {
        if (canCopyTagDeclaration(enumRef)) {
            enumRef.setDeclaration(copyController.copy(enumRef.getDeclaration()));
        }
        return null;
    }

    private boolean canCopyTagDeclaration(TagRef tagRef) {
        return !VariousUtils.getBooleanValue(tagRef.getIsPasted()) && tagRef.getNestedInNescEntity();
    }

    @Override
    public Void visitIdLabel(IdLabel label, Void arg) {
        label.setDeclaration(copyController.copy(label.getDeclaration()));
        return null;
    }

    @Override
    public Void visitInterfaceRef(InterfaceRef interfaceRef, Void arg) {
        interfaceRef.setDeclaration(copyController.copy(interfaceRef.getDeclaration()));
        return null;
    }

    @Override
    public Void visitComponentRef(ComponentRef componentRef, Void arg) {
        componentRef.setDeclaration(copyController.copy(componentRef.getDeclaration()));
        return null;
    }

    @Override
    public Void visitNescAttribute(NescAttribute attribute, Void arg) {
        attribute.setDeclaration(copyController.copy(attribute.getDeclaration()));
        return null;
    }

    @Override
    public Void visitTypeParmDecl(TypeParmDecl declaration, Void arg) {
        declaration.setDeclaration(copyController.copy(declaration.getDeclaration()));
        return null;
    }

    @Override
    public Void visitPlus(Plus expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitMinus(Minus expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitTimes(Times expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitDivide(Divide expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitModulo(Modulo expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitLshift(Lshift expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitRshift(Rshift expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitLeq(Leq expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitGeq(Geq expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitLt(Lt expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitGt(Gt expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitEq(Eq expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitNe(Ne expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitBitand(Bitand expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitBitor(Bitor expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitBitxor(Bitxor expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitAndand(Andand expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitOror(Oror expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitAssign(Assign expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitPlusAssign(PlusAssign expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitMinusAssign(MinusAssign expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitTimesAssign(TimesAssign expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitDivideAssign(DivideAssign expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitModuloAssign(ModuloAssign expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitLshiftAssign(LshiftAssign expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitRshiftAssign(RshiftAssign expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitBitandAssign(BitandAssign expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitBitorAssign(BitorAssign expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitBitxorAssign(BitxorAssign expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitUnaryMinus(UnaryMinus expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitDereference(Dereference expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitAddressOf(AddressOf expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitUnaryPlus(UnaryPlus expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitBitnot(Bitnot expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitNot(Not expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitAlignofType(AlignofType expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitSizeofType(SizeofType expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitOffsetof(Offsetof expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitSizeofExpr(SizeofExpr expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitAlignofExpr(AlignofExpr expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitRealpart(Realpart expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitImagpart(Imagpart expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitArrayRef(ArrayRef expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitErrorExpr(ErrorExpr expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitComma(Comma expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitLabelAddress(LabelAddress expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitConditional(Conditional expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitIdentifier(Identifier expr, Void arg) {
        if (expr.getRefsDeclInThisNescEntity() && !VariousUtils.getBooleanValue(expr.getIsPasted())) {
            expr.setDeclaration(copyController.copy(expr.getDeclaration()));
        }

        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitCompoundExpr(CompoundExpr expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitIntegerCst(IntegerCst expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitFloatingCst(FloatingCst expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitCharacterCst(CharacterCst expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitStringCst(StringCst expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitStringAst(StringAst expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitFunctionCall(FunctionCall expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitUniqueCall(UniqueCall expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitUniqueNCall(UniqueNCall expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitUniqueCountCall(UniqueCountCall expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitFieldRef(FieldRef expr, Void arg) {
        expr.setDeclaration(copyController.copy(expr.getDeclaration()));
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitInterfaceDeref(InterfaceDeref expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitComponentDeref(ComponentDeref expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitPreincrement(Preincrement expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitPredecrement(Predecrement expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitPostincrement(Postincrement expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitPostdecrement(Postdecrement expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitCast(Cast expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitCastList(CastList expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitTypeArgument(TypeArgument expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitGenericCall(GenericCall expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    @Override
    public Void visitExtensionExpr(ExtensionExpr expr, Void arg) {
        substituteExprType(expr);
        return arg;
    }

    private void substituteExprType(Expression expr) {
        if (expr.getType().isPresent()) {
            expr.setType(Optional.of(expr.getType().get().accept(this, null)));
        }
    }

    @Override
    public Void visitAstType(AstType astType, Void arg) {
        if (astType.getType().isPresent()) {
            astType.setType(Optional.of(astType.getType().get().accept(this, null)));
        }
        return arg;
    }

    @Override
    public Type visit(ArrayType type, Void arg) {
        final Optional<Expression> size;

        if (type.getSize().isPresent()) {
            size = nodesMap.containsKey(type.getSize().get())
                    ? Optional.of((Expression) nodesMap.get(type.getSize().get()))
                    : Optional.<Expression>absent();
        } else {
            size = Optional.absent();
        }

        return new ArrayType(type.getElementType().accept(this, null), size);
    }

    @Override
    public Type visit(CharType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(SignedCharType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(UnsignedCharType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(ShortType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(UnsignedShortType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(IntType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(UnsignedIntType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(LongType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(UnsignedLongType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(LongLongType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(UnsignedLongLongType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(FloatType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(DoubleType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(LongDoubleType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(EnumeratedType type, Void arg) {
        return type.getEnumDeclaration().getAstNode().getNestedInNescEntity()
                ? new EnumeratedType(type.isConstQualified(), type.isVolatileQualified(),
                    copyController.copy(type.getEnumDeclaration()))
                : type;
    }

    @Override
    public Type visit(PointerType type, Void arg) {
        return new PointerType(type.isConstQualified(), type.isVolatileQualified(),
                type.isRestrictQualified(), type.getReferencedType().accept(this, null));
    }

    @Override
    public Type visit(FunctionType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(StructureType type, Void arg) {
        return type.getDeclaration().getAstNode().getNestedInNescEntity()
                ? new StructureType(type.isConstQualified(), type.isVolatileQualified(),
                    copyController.copy(type.getDeclaration()))
                : type;
    }

    @Override
    public Type visit(UnionType type, Void arg) {
        return type.getDeclaration().getAstNode().getNestedInNescEntity()
                ? new UnionType(type.isConstQualified(), type.isVolatileQualified(),
                        copyController.copy(type.getDeclaration()))
                : type;
    }

    @Override
    public Type visit(ExternalStructureType type, Void arg) {
        return type.getDeclaration().getAstNode().getNestedInNescEntity()
                ? new ExternalStructureType(type.isConstQualified(), type.isVolatileQualified(),
                        copyController.copy(type.getDeclaration()))
                : type;
    }

    @Override
    public Type visit(ExternalUnionType type, Void arg) {
        return type.getDeclaration().getAstNode().getNestedInNescEntity()
                ? new ExternalUnionType(type.isConstQualified(), type.isVolatileQualified(),
                    copyController.copy(type.getDeclaration()))
                : type;
    }

    @Override
    public Type visit(VoidType type, Void arg) {
        return type;
    }

    @Override
    public Type visit(UnknownType type, Void arg) {
        if (UnknownType.unnamed().getName().equals(type.getName())) {
            return type;
        }
        return prepareUnknownType(type);
    }

    @Override
    public Type visit(UnknownArithmeticType type, Void arg) {
        if (UnknownArithmeticType.unnamed().getName().equals(type.getName())) {
            return type;
        }
        return prepareUnknownType(type);
    }

    @Override
    public Type visit(UnknownIntegerType type, Void arg) {
        if (UnknownIntegerType.unnamed().getName().equals(type.getName())) {
            return type;
        }
        return prepareUnknownType(type);
    }

    @Override
    public Type visit(InterfaceType type, Void arg) {
        throw new RuntimeException("unexpected artificial type");
    }

    @Override
    public Type visit(TypeDefinitionType type, Void arg) {
        throw new RuntimeException("unexpected artificial type");
    }

    @Override
    public Type visit(ComponentType type, Void arg) {
        throw new RuntimeException("unexpected artificial type");
    }


    private Type prepareUnknownType(UnknownType unknownType) {
        final Type destType = typesMap.get(unknownType.getName());
        checkNotNull(destType, "type parameter '%s' has not got any replacement",
                unknownType.getName());
        return destType;
    }

    /**
     * Builder for particular elements of the visitor.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class PrivateBuilder {
        private final ComponentRef componentRef;
        private final Component genericComponent;

        private PrivateBuilder(ComponentRef componentRef, Component genericComponent) {
            this.genericComponent = genericComponent;
            this.componentRef = componentRef;
        }

        private ImmutableMap<String, Type> buildTypesMap() {
            final ImmutableMap.Builder<String, Type> typesBuilder = ImmutableMap.builder();
            final Iterator<Expression> paramsIt = componentRef.getArguments().iterator();
            final Iterator<Declaration> paramsDeclsIt = genericComponent.getParameters().get().iterator();

            while (paramsDeclsIt.hasNext()) {
                final DataDecl dataDecl = (DataDecl) paramsDeclsIt.next();
                final Expression parameter = paramsIt.next();

                if (TypeElementUtils.isTypedef(dataDecl.getModifiers())) {
                    final String name = DeclaratorUtils.getDeclaratorName(((VariableDecl) dataDecl
                            .getDeclarations().getFirst()).getDeclarator().get()).get();
                    final TypeArgument typeParam = (TypeArgument) parameter;
                    final Type targetType = typeParam.getAsttype().getType().get();
                    checkState(!targetType.isUnknownType(), "substituting a type parameter for an unknown type");
                    typesBuilder.put(name, typeParam.getAsttype().getType().get());
                }
            }

            return typesBuilder.build();
        }
    }
}
