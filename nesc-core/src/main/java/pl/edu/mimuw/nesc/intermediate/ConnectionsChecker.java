package pl.edu.mimuw.nesc.intermediate;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import pl.edu.mimuw.nesc.ast.InstantiationOrigin;
import pl.edu.mimuw.nesc.ast.IntermediateData;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionCall;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.compilation.CompilationListener;
import pl.edu.mimuw.nesc.problem.NescError;
import pl.edu.mimuw.nesc.problem.issue.NotImplementedEntityError;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class for checking if all called used commands or signalled used events are
 * properly implemented, i.e. if they are connected to an implementation or
 * a default implementation is provided for them.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ConnectionsChecker {
    /**
     * Instantiation chain for each instantiated component.
     */
    private final ImmutableMap<String, ImmutableList<InstantiationOrigin>> instantiationChains;

    /**
     * Iterable with all C declarations that constitute the NesC program that
     * will be checked.
     */
    private final Iterable<Declaration> allDeclarations;

    /**
     * Object notified about all detected issues.
     */
    private final CompilationListener listener;

    /**
     * Construct a connections checker that will use given instantiation chains
     * for determining if a component has been instantiated and in messages for
     * issues. All detected issues are reported to the given listener.
     *
     * @param instantiationChains Instantiation chains for all instantiated
     *                            components.
     * @param allDeclarations Iterable with all declarations that constitute the
     *                        NesC program that will be checked. It is required
     *                        that method {@link Iterable#iterator} returns
     *                        always a new instance of the iterator.
     * @param listener Object that will be notified about all detected issues.
     */
    public ConnectionsChecker(ImmutableMap<String, ImmutableList<InstantiationOrigin>> instantiationChains,
            Iterable<Declaration> allDeclarations, CompilationListener listener) {
        checkNotNull(instantiationChains, "instantiation chains cannot be null");
        checkNotNull(allDeclarations, "declarations cannot be null");
        checkNotNull(listener, "listener cannot be null");
        this.instantiationChains = instantiationChains;
        this.allDeclarations = allDeclarations;
        this.listener = listener;
    }

    /**
     * Check if all called used commands and all signalled used events are
     * properly implemented. If no, the listener is informed about errors.
     */
    public void check() {
        final ImmutableMap<String, IntermediateData> intermediateFunctions =
                collectIntermediateData();
        final ControllingVisitor controllingVisitor = new ControllingVisitor(intermediateFunctions);

        for (Declaration declaration : allDeclarations) {
            declaration.accept(controllingVisitor, null);
        }
    }

    private ImmutableMap<String, IntermediateData> collectIntermediateData() {
        final ImmutableMap.Builder<String, IntermediateData> intermediateFunctionsBuilder =
                ImmutableMap.builder();

        for (Declaration declaration : allDeclarations) {
            while (declaration instanceof ExtensionDecl) {
                declaration = ((ExtensionDecl) declaration).getDeclaration();
            }
            if (declaration instanceof FunctionDecl) {
                final FunctionDecl function = (FunctionDecl) declaration;
                final Optional<IntermediateData> intermediateData = function.getIntermediateData();

                if (intermediateData != null && intermediateData.isPresent()) {
                    intermediateFunctionsBuilder.put(DeclaratorUtils.getUniqueName(
                            function.getDeclarator()).get(), intermediateData.get());
                }
            }
        }

        return intermediateFunctionsBuilder.build();
    }

    /**
     * Visitor that decided which declarations will be checked and if so
     * delegates the checking.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class ControllingVisitor extends ExceptionVisitor<Void, Void> {
        private final ImmutableMap<String, IntermediateData> intermediateFunctions;
        private final CheckingVisitor checkingVisitor;

        private ControllingVisitor(ImmutableMap<String, IntermediateData> intermediateFunctions) {
            checkNotNull(intermediateFunctions, "intermediate functions cannot be null");
            this.intermediateFunctions = intermediateFunctions;
            this.checkingVisitor = new CheckingVisitor(intermediateFunctions);
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            final String funUniqueName = DeclaratorUtils.getUniqueName(
                    declaration.getDeclarator()).get();
            if (!intermediateFunctions.containsKey(funUniqueName)) {
                /* Check the function only if it is not an intermediate
                   function. */
                declaration.traverse(checkingVisitor, null);
            }
            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            declaration.traverse(checkingVisitor, null);
            return null;
        }
    }

    /**
     * Visitor that actually checks calls to intermediate functions and notifies
     * the listener about issues.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class CheckingVisitor extends IdentityVisitor<Void> {
        private final ImmutableMap<String, IntermediateData> intermediateFunctions;

        private CheckingVisitor(ImmutableMap<String, IntermediateData> intermediateFunctions) {
            checkNotNull(intermediateFunctions, "intermediate functions cannot be null");
            this.intermediateFunctions = intermediateFunctions;
        }

        @Override
        public Void visitFunctionCall(FunctionCall call, Void arg) {
            if (!(call.getFunction() instanceof Identifier)) {
                return null;
            }

            final Identifier calledFun = (Identifier) call.getFunction();
            if (calledFun.getUniqueName() == null || !calledFun.getUniqueName().isPresent()) {
                return null;
            }

            final String calledFunUniqueName = calledFun.getUniqueName().get();
            final Optional<IntermediateData> intermediateData = Optional.fromNullable(
                    intermediateFunctions.get(calledFunUniqueName));

            if (intermediateData.isPresent() && !intermediateData.get().isImplemented()) {
                /* An used command is called or an used event is signalled but
                   it is not implemented. It is a compile-time error. */
                final NotImplementedEntityError error;
                final Optional<ImmutableList<InstantiationOrigin>> instantiationChain =
                        Optional.fromNullable(instantiationChains.get(intermediateData.get().getComponentName()));
                if (instantiationChain.isPresent()) {
                    error = NotImplementedEntityError.entityFromInstantiatedComponent(
                            instantiationChain.get(),
                            intermediateData.get().getInterfaceRefName(),
                            intermediateData.get().getEntityName(),
                            intermediateData.get().getKind()
                    );
                } else {
                    error = NotImplementedEntityError.entityFromNormalComponent(
                            intermediateData.get().getComponentName(),
                            intermediateData.get().getInterfaceRefName(),
                            intermediateData.get().getEntityName(),
                            intermediateData.get().getKind()
                    );
                }

                listener.error(new NescError(call.getLocation(), Optional.of(call.getEndLocation()),
                        Optional.of(error.getCode()), error.generateDescription()));
            }

            return null;
        }
    }
}
