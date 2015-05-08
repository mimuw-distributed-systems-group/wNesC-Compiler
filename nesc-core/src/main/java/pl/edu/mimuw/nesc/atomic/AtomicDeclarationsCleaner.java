package pl.edu.mimuw.nesc.atomic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionCall;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.Typename;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.refsgraph.EntityNode;
import pl.edu.mimuw.nesc.refsgraph.Reference;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Cleaner for atomic declarations: the type definition and functions for
 * starting an atomic statement and ending it. If such declarations cannot be
 * removed it is responsible for updating the references graph.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AtomicDeclarationsCleaner {
    /**
     * Declarations with atomic declarations and potential references to them.
     */
    private final ImmutableList<Declaration> declarations;

    /**
     * References graph to update
     */
    private final ReferencesGraph refsGraph;

    /**
     * Specification of atomic assumed in the program.
     */
    private final AtomicSpecification atomicSpecification;

    public AtomicDeclarationsCleaner(ImmutableList<Declaration> declarations, ReferencesGraph refsGraph,
            AtomicSpecification atomicSpec) {
        checkNotNull(declarations, "declarations cannot be null");
        checkNotNull(refsGraph, "references grpah cannot be null");
        checkNotNull(atomicSpec, "atomic specification cannot be null");
        this.declarations = declarations;
        this.refsGraph = refsGraph;
        this.atomicSpecification = atomicSpec;
    }

    /**
     * Performs the cleaning operation. If the atomic declarations: type
     * definition and functions for atomic, are not referenced they are
     * removed. Otherwise, they are preserved and the references graph is
     * updated.
     *
     * @return Declarations after cleaning in proper order.
     */
    public ImmutableList<Declaration> clean() {
        // Update references graph
        final ReferencesGraphUpdatingVisitor updatingVisitor = new ReferencesGraphUpdatingVisitor();
        for (Declaration declaration : declarations) {
            if (declaration instanceof FunctionDecl) {
                final FunctionDecl funDecl = (FunctionDecl) declaration;
                funDecl.traverse(updatingVisitor, DeclaratorUtils.getUniqueName(
                        funDecl.getDeclarator()).get());
            }
        }

        final ImmutableSet<String> forRemoval = determineAtomicDeclsForRemoval();

        // Remove atomic declarations
        return forRemoval.isEmpty()
                ? declarations
                : removeAtomicDeclarations(forRemoval);
    }

    private ImmutableSet<String> getSpontaneousAtomicDecls() {
        final ImmutableSet.Builder<String> spontaneousDeclsBuilder = ImmutableSet.builder();

        for (Declaration declaration : declarations) {
            while (declaration instanceof ExtensionDecl) {
                declaration = ((ExtensionDecl) declaration).getDeclaration();
            }

            if (declaration instanceof FunctionDecl) {
                final FunctionDecl funDecl = (FunctionDecl) declaration;
                final String uniqueName = DeclaratorUtils.getUniqueName(funDecl.getDeclarator()).get();

                if (!uniqueName.equals(atomicSpecification.getStartFunctionName())
                        && !uniqueName.equals(atomicSpecification.getEndFunctionName())) {
                    continue;
                }

                if (funDecl.getDeclaration() != null && funDecl.getDeclaration().getCallAssumptions()
                            .compareTo(FunctionDeclaration.CallAssumptions.SPONTANEOUS) >= 0) {
                    spontaneousDeclsBuilder.add(uniqueName);
                }
            }
        }

        return spontaneousDeclsBuilder.build();
    }

    private ImmutableSet<String> determineAtomicDeclsForRemoval() {
        final ImmutableSet<String> spontaneousAtomicDecls = getSpontaneousAtomicDecls();
        final ImmutableSet<String> removalCandidates = ImmutableSet.of(
                atomicSpecification.getTypename(),
                atomicSpecification.getStartFunctionName(),
                atomicSpecification.getEndFunctionName()
        );
        final Set<String> removedDecls = new HashSet<>();
        int initialSize;

        do {
            initialSize = removedDecls.size();

            // Prepare candidates for removal
            final Set<String> forRemoval = new HashSet<>(removalCandidates);
            forRemoval.removeAll(removedDecls);
            forRemoval.removeAll(spontaneousAtomicDecls);

            // Check candidates
            final Iterator<String> forRemovalIt = forRemoval.iterator();
            while (forRemovalIt.hasNext()) {
                final EntityNode atomicDeclarationNode = refsGraph.getOrdinaryIds().get(forRemovalIt.next());
                if (!atomicDeclarationNode.getPredecessors().isEmpty()) {
                    forRemovalIt.remove();
                }
            }

            for (String newRemovedDecl : forRemoval) {
                removedDecls.add(newRemovedDecl);
                refsGraph.removeOrdinaryId(newRemovedDecl);
            }
        } while (initialSize != removedDecls.size());

        return ImmutableSet.copyOf(removedDecls);
    }

    private ImmutableList<Declaration> removeAtomicDeclarations(ImmutableSet<String> forRemoval) {
        final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();
        final RemoveControllingVisitor visitor = new RemoveControllingVisitor(forRemoval);

        for (Declaration declaration : declarations) {
            if (!declaration.accept(visitor, null)) {
                declarationsBuilder.add(declaration);
            }
        }

        return declarationsBuilder.build();
    }

    /**
     * Visitor that looks at references to atomic declarations that have atomic
     * origin and updates the references graph.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class ReferencesGraphUpdatingVisitor extends IdentityVisitor<String> {
        @Override
        public String visitTypename(Typename typeElement, String funName) {
            if (!VariousUtils.getBooleanValue(typeElement.getHasAtomicOrigin())) {
                return funName;
            }

            if (typeElement.getUniqueName().equals(atomicSpecification.getTypename())) {
                refsGraph.addReferenceOrdinaryToOrdinary(funName, typeElement.getUniqueName(),
                        Reference.Type.NORMAL, typeElement, false, true);
            }

            return funName;
        }

        @Override
        public String visitFunctionCall(FunctionCall expr, String funName) {
            if (!VariousUtils.getBooleanValue(expr.getHasAtomicOrigin())) {
                return funName;
            }

            final Identifier funIdent = (Identifier) expr.getFunction();
            if (!funIdent.getUniqueName().isPresent()) {
                return funName;
            }

            if (funIdent.getUniqueName().get().equals(atomicSpecification.getStartFunctionName())
                    || funIdent.getUniqueName().get().equals(atomicSpecification.getEndFunctionName())) {
                refsGraph.addReferenceOrdinaryToOrdinary(funName, funIdent.getUniqueName().get(),
                        Reference.Type.CALL, expr, false, true);
            }

            return funName;
        }
    }

    /**
     * Visitor that decides if an atomic declaration is to be removed. It
     * returns <code>true</code> in such case.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class RemoveControllingVisitor extends ExceptionVisitor<Boolean, Void> {
        private final ImmutableSet<String> forRemoval;

        private RemoveControllingVisitor(ImmutableSet<String> forRemoval) {
            this.forRemoval = forRemoval;
        }

        @Override
        public Boolean visitDataDecl(DataDecl declaration, Void arg) {
            // Remove inner declarations
            final Iterator<Declaration> declsIt = declaration.getDeclarations().iterator();
            while (declsIt.hasNext()) {
                if (declsIt.next().accept(this, null)) {
                    declsIt.remove();
                }
            }

            for (TypeElement typeElement : declaration.getModifiers()) {
                if (typeElement instanceof TagRef) {
                    return false;
                }
            }

            return declaration.getDeclarations().isEmpty();
        }

        @Override
        public Boolean visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            return declaration.getDeclaration().accept(this, null);
        }

        @Override
        public Boolean visitFunctionDecl(FunctionDecl declaration, Void arg) {
            return forRemoval.contains(DeclaratorUtils.getUniqueName(
                    declaration.getDeclarator()).get());
        }

        @Override
        public Boolean visitVariableDecl(VariableDecl declaration, Void arg) {
            return forRemoval.contains(DeclaratorUtils.getUniqueName(
                    declaration.getDeclarator().get()).get());
        }
    }
}
