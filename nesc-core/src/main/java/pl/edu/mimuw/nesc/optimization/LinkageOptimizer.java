package pl.edu.mimuw.nesc.optimization;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Object that changes linkage of global variables to internal and makes
 * functions static inline if it is possible.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class LinkageOptimizer {
    /**
     * Set with names of variables whose external linkage must not be changed.
     */
    private final Set<String> externalVariables;

    /**
     * Name mangler used to generate unique names if it is necessary.
     */
    private final NameMangler nameMangler;

    /**
     * Initialize this linkage optimizer to use given set of names of external
     * variables during optimization.
     *
     * @param externalVariables Set with names of variables whose linkage will
     *                          not be changed from external to internal.
     * @param nameMangler Name mangler for generation of unique names for
     *                    unnamed tags if it is necessary.
     */
    public LinkageOptimizer(Set<String> externalVariables, NameMangler nameMangler) {
        checkNotNull(externalVariables, "external variables cannot be null");
        checkNotNull(nameMangler, "name mangler cannot be null");
        this.externalVariables = externalVariables;
        this.nameMangler = nameMangler;
    }

    /**
     * Optimize the given declarations.
     *
     * @param declarations List with declarations to be optimized.
     * @return List with declarations after performing the linkage optimization.
     */
    public ImmutableList<Declaration> optimize(List<Declaration> declarations) {
        checkNotNull(declarations, "declarations cannot be null");

        final ImmutableList<Declaration> separatedDecls =
                AstUtils.separateDeclarations(declarations, nameMangler);
        final OptimizerVisitor visitor = new OptimizerVisitor();

        for (Declaration declaration : separatedDecls) {
            declaration.accept(visitor, null);
        }

        return separatedDecls;
    }

    private void makeStaticInline(List<TypeElement> typeElements) {
        final EnumSet<RID> specifiers = TypeElementUtils.collectRID(typeElements);

        if (specifiers.contains(RID.EXTERN)) {
            TypeElementUtils.removeRID(typeElements, RID.EXTERN);
        }

        if (!specifiers.contains(RID.INLINE)) {
            typeElements.add(0, AstUtils.newRid(RID.INLINE));
        }

        if (!specifiers.contains(RID.STATIC)) {
            typeElements.add(0, AstUtils.newRid(RID.STATIC));
        }
    }

    /**
     * Visitor that actually modifies the linkage.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class OptimizerVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Set with names of functions whose linkage is to be internal.
         */
        private final Set<String> internalFunctionsNames = new HashSet<>();

        /**
         * Multimap with forward declarations of functions.
         */
        private final ListMultimap<String, DataDecl> functionsDeclarations = ArrayListMultimap.create();

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            /* Don't affect the linkage if there are call assumptions stronger
               than NONE. */

            final FunctionDeclaration functionDeclaration = declaration.getDeclaration();
            if (functionDeclaration != null && functionDeclaration.getCallAssumptions().compareTo(
                    FunctionDeclaration.CallAssumptions.NONE) > 0) {
                return null;
            }

            // Change the linkage to internal

            final String funName = DeclaratorUtils.getUniqueName(declaration.getDeclarator()).get();
            makeStaticInline(declaration.getModifiers());

            for (DataDecl forwardDecl : functionsDeclarations.get(funName)) {
                makeStaticInline(forwardDecl.getModifiers());
            }

            internalFunctionsNames.add(funName);

            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            final EnumSet<RID> specifiers = TypeElementUtils.collectRID(declaration.getModifiers());

            if (specifiers.contains(RID.TYPEDEF) || declaration.getDeclarations().isEmpty()) {
                return null;
            }

            final VariableDecl variableDecl = (VariableDecl) declaration.getDeclarations().getFirst();
            final String name = DeclaratorUtils.getUniqueName(variableDecl.getDeclarator()).get();
            final Optional<NestedDeclarator> deepestNestedDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(variableDecl.getDeclarator());

            if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof FunctionDeclarator) {
                // Forward declaration of function
                if (internalFunctionsNames.contains(name)) {
                    makeStaticInline(declaration.getModifiers());
                } else {
                    functionsDeclarations.put(name, declaration);
                }
            } else if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof InterfaceRefDeclarator) {
                throw new RuntimeException("unexpected interface reference declarator");
            } else {
                // Variable declaration
                /* FIXME multiple declarations of the same global variable with
                   different storage-class specifiers (currently not accepted by
                   the compiler). */
                if (!externalVariables.contains(name) && !specifiers.contains(RID.EXTERN)
                        && !specifiers.contains(RID.STATIC) && !specifiers.contains(RID.AUTO)
                        && !specifiers.contains(RID.REGISTER)) {
                    declaration.getModifiers().addFirst(AstUtils.newRid(RID.STATIC));
                }
            }

            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }
    }
}
