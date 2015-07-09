package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Object responsible for giving names to unnamed parameters in functions
 * definitions.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ParametersNamesGiver {
    /**
     * Object that will be used to generating unique names.
     */
    private final NameMangler nameMangler;

    /**
     * Visitor used by this object to give names.
     */
    private final NamingVisitor namingVisitor;

    /**
     * Create a parameters names giver that will use given name mangler for
     * creation of the unique names.
     *
     * @param nameMangler Name mangler that will be used.
     */
    public ParametersNamesGiver(NameMangler nameMangler) {
        checkNotNull(nameMangler, "name mangler cannot be null");
        this.nameMangler = nameMangler;
        this.namingVisitor = new NamingVisitor();
    }

    /**
     * Name unnamed parameters from all functions definitions contained in the
     * given iterable and inside declarations from the iterable.
     *
     * @param declarations Declarations with definitions of functions whose
     *                     unnamed parameters will be named.
     */
    public void name(Iterable<Declaration> declarations) {
        checkNotNull(declarations, "declarations cannot be null");
        for (Declaration declaration : declarations) {
            name(declaration);
        }
    }

    /**
     * Name unnamed parameters in functions definitions contained in the given
     * declaration. If the given declaration is a function definition, its
     * parameters are also named.
     *
     * @param declaration Declaration with possible definitions of functions
     *                    whose unnamed parameters will be named. It also
     *                    applies to the declaration itself it is a function
     *                    definition.
     */
    public void name(Declaration declaration) {
        checkNotNull(declaration, "declaration cannot be null");
        declaration.traverse(namingVisitor, null);
    }

    /**
     * Visitor that will actually name unnamed parameters in functions
     * definitions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class NamingVisitor extends IdentityVisitor<Void> {
        @Override
        public Void visitFunctionDecl(FunctionDecl function, Void arg) {
            final Optional<NestedDeclarator> deepestNestedDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(function.getDeclarator());

            if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof FunctionDeclarator) {
                final FunctionDeclarator functionDeclarator =
                         (FunctionDeclarator) deepestNestedDeclarator.get();
                AstUtils.nameEntities(functionDeclarator.getParameters(), nameMangler);
                if (functionDeclarator.getGenericParameters().isPresent()) {
                    throw new RuntimeException("unexpected generic parameters");
                }
                return null;
            } else {
                throw new RuntimeException("expected function declarator");
            }
        }
    }
}
