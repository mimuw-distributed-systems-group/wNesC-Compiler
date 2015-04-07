package pl.edu.mimuw.nesc.analysis.expressions;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;
import pl.edu.mimuw.nesc.astutil.ExpressionUtils;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.declaration.object.VariableDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Objects responsible for analysis of expressions that are arguments for
 * attributes. Analysis of an expression has the following side effects:</p>
 *
 * <ul>
 *     <li>{@link Identifier#isGenericReference} of an identifier is set
 *     according to the provided environment; if the identifier is absent in the
 *     environment, the flag is set to <code>false</code></li>
 *     <li>{@link Identifier#refsDeclInThisNescEntity} of an identifier is set
 *     according to the provided environment</li>
 *     <li>{@link Expression#type} of all expressions contained in the given
 *     expression and in itself is set to an absent object</li>
 *     <li>{@link Identifier#uniqueName} of an identifier is set to an absent
 *     object</li>
 * </ul>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AttributeExpressionAnalyzer {
    /**
     * Analyzes the given expression by performing steps described in
     * description of the class.
     *
     * @param expr Expression that is an argument for an attribute to analyze.
     * @param environment Environment of the given expression.
     * @see AttributeExpressionAnalyzer
     */
    public void analyze(Expression expr, Environment environment) {
        checkNotNull(expr, "expression cannot be null");
        checkNotNull(environment, "environment cannot be null");

        ExpressionUtils.setTypeDeep(expr, Optional.<Type>absent());
        ExpressionUtils.setUniqueNameDeep(expr, Optional.<String>absent());
        expr.traverse(new GenericReferenceSettingVisitor(environment), null);
    }

    /**
     * Analyzes the given expression if it is present.
     *
     * @param expr Expression that is an argument for an attribute to analyze.
     * @param environment Environment of the given expression.
     */
    public void analyze(Optional<Expression> expr, Environment environment) {
        checkNotNull(expr, "expression cannot be null");
        if (expr.isPresent()) {
            analyze(expr.get(), environment);
        }
    }

    /**
     * Analyzes all of expressions from the given iterable.
     *
     * @param exprs Iterable of expressions to analyze.
     * @param environment Environment of the expressions.
     */
    public void analyze(Iterable<? extends Expression> exprs, Environment environment) {
        checkNotNull(exprs, "expressions cannot be null");
        for (Expression expr : exprs) {
            analyze(expr, environment);
        }
    }

    /**
     * Visitor that is responsible for checking and setting values of field
     * {@link Identifier#isGenericReference} in visited identifiers.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class GenericReferenceSettingVisitor extends IdentityVisitor<Void> {
        /**
         * Environment to look for the information.
         */
        private final Environment environment;

        private GenericReferenceSettingVisitor(Environment environment) {
            checkNotNull(environment, "environment cannot be null");
            this.environment = environment;
        }

        @Override
        public Void visitIdentifier(Identifier identifier, Void arg) {
            final Optional<? extends ObjectDeclaration> optDeclaration =
                    environment.getObjects().get(identifier.getName());
            final boolean isGenericReference;

            if (!optDeclaration.isPresent()) {
                isGenericReference = false;
            } else {
                final ObjectDeclaration declaration = optDeclaration.get();
                switch (declaration.getKind()) {
                    case TYPENAME:
                        isGenericReference = ((TypenameDeclaration) declaration).isGenericParameter();
                        break;
                    case VARIABLE:
                        isGenericReference = ((VariableDeclaration) declaration).isGenericParameter();
                        break;
                    default:
                        isGenericReference = false;
                        break;
                }
            }

            identifier.setIsGenericReference(isGenericReference);
            identifier.setRefsDeclInThisNescEntity(environment.isObjectDeclaredInsideNescEntity(
                    identifier.getName()));

            return null;
        }
    }
}
