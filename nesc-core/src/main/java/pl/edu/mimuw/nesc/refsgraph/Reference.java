package pl.edu.mimuw.nesc.refsgraph;

import pl.edu.mimuw.nesc.ast.gen.FunctionCall;
import pl.edu.mimuw.nesc.ast.gen.Node;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that represents a reference of another entity. It can be thought of
 * an edge in the references graph.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Reference {
    /**
     * Node that is references the referenced node - the tail of the edge.
     */
    private final EntityNode referencingNode;

    /**
     * Node that is referenced - the head of the edge.
     */
    private final EntityNode referencedNode;

    /**
     * Type of this reference.
     */
    private final Type type;

    /**
     * AST node that is the reference.
     */
    private final Node astNode;

    /**
     * Value indicating if this reference occurs inside an expression that is
     * not evaluated, i.e. inside <code>sizeof</code> or <code>_Alignof</code>
     * operators.
     */
    private final boolean insideNotEvaluatedExpr;

    /**
     * Value indicating if this reference occurs inside a part of code that is
     * known to execute atomically.
     */
    private final boolean insideAtomic;

    /**
     * Initializes this reference by storing values of given parameters in
     * member fields.
     *
     * @throws IllegalArgumentException The type is {@link Type#CALL} but the
     *                                  referenced node is neither a function nor
     *                                  a variable (a variable can be called if
     *                                  it is a pointer) or the AST node is not
     *                                  a function call.
     */
    Reference(EntityNode referencingNode, EntityNode referencedNode, Type type,
            Node astNode, boolean insideNotEvaluatedExpr, boolean insideAtomic) {
        checkNotNull(referencingNode, "referencing node cannot be null");
        checkNotNull(referencedNode, "referenced node cannot be null");
        checkNotNull(type, "type cannot be null");
        checkNotNull(astNode, "AST node cannot be null");
        checkArgument(type != Type.CALL || referencedNode.getKind() == EntityNode.Kind.FUNCTION
            || referencedNode.getKind() == EntityNode.Kind.VARIABLE,
            "the type of this reference is call but the referenced node is not a function and is not a variable");
        checkArgument(type != Type.CALL || astNode instanceof FunctionCall,
            "the type of this reference is call but the AST node is not a function call");

        this.referencingNode = referencingNode;
        this.referencedNode = referencedNode;
        this.type = type;
        this.astNode = astNode;
        this.insideNotEvaluatedExpr = insideNotEvaluatedExpr;
        this.insideAtomic = insideAtomic;
    }

    /**
     * Get the node that is referencing the referenced node.
     *
     * @return Node that references the referenced node.
     */
    public EntityNode getReferencingNode() {
        return referencingNode;
    }

    /**
     * Get the node that is referenced.
     *
     * @return Node of the referenced entity.
     */
    public EntityNode getReferencedNode() {
        return referencedNode;
    }

    /**
     * Get the type of this reference.
     *
     * @return Type of this reference.
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the AST node that is the reference.
     *
     * @return AST node that is the reference. If this reference has type CALL,
     *         it is guaranteed that the returned node is
     *         <code>FunctionCall</code>.
     */
    public Node getASTNode() {
        return astNode;
    }

    /**
     * Check if this reference occurred inside an expression that is not
     * evaluated, i.e. inside the argument for <code>sizeof</code> or
     * <code>_Alignof</code> operators.
     *
     * @return <code>true</code> if and only if this reference occurs inside
     *         an expression that is not evaluated.
     */
    public boolean isInsideNotEvaluatedExpr() {
        return insideNotEvaluatedExpr;
    }

    /**
     * Check if this reference occurred inside a part of code that is known
     * to execute atomially, e.g. in an atomic statement or in a function with
     * call assumptions equal to
     * {@link pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration.CallAssumptions#ATOMIC_HWEVENT
     * ATOMIC_HWEVENT}.
     *
     * @return <code>true</code> if and only if this reference occurs inside
     *         a part of code that is executed atomically.
     */
    public boolean isInsideAtomic() {
        return insideAtomic;
    }

    /**
     * <p>Enum type that represents a type of reference of an entity.</p>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum Type {
        /**
         * Normal occurrence of an identifier, other than call of a function.
         */
        NORMAL,
        /**
         * The referenced identifier is a function that is called.
         */
        CALL,
    }
}
