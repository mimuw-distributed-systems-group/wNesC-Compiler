package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Object responsible for copying type elements from the top-declarations if
 * necessary and then preserving them in intact state.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TypeElementsPreserver {
    /**
     * The adjuster used by this preserver.
     */
    private final TypeElementsAdjuster adjuster;

    /**
     * Data that enables restoring original state of AST nodes of the NesC
     * program.
     */
    private final Map<Node, OriginalState> astPreservationData;

    /**
     * Construct a preserver that uses the given adjuster for the adjustment
     * operations.
     *
     * @param adjuster The adjuster used by this preserver.
     */
    public TypeElementsPreserver(TypeElementsAdjuster adjuster) {
        checkNotNull(adjuster, "adjuster cannot be null");
        this.adjuster = adjuster;
        this.astPreservationData = new HashMap<>();
    }

    /**
     * Adjust declarations on the given list using the adjuster of this
     * preserver.
     *
     * @param topLevelDeclarations Declarations to adjust. They should be
     *                             separated.
     */
    public void adjust(ImmutableList<Declaration> topLevelDeclarations) {
        checkNotNull(topLevelDeclarations, "top-level declarations cannot be null");
        final AdjustingVisitor adjustingVisitor = new AdjustingVisitor();
        for (Declaration declaration : topLevelDeclarations) {
            declaration.accept(adjustingVisitor, null);
        }
    }

    /**
     * Restore type elements of the given declarations to their original state.
     *
     * @param topLevelDeclarations Declarations whose state will be restored to
     *                             the original one.
     */
    public void restore(ImmutableList<Declaration> topLevelDeclarations) {
        checkNotNull(topLevelDeclarations, "top-level declarations cannot be null");
        final RestoringVisitor restoringVisitor = new RestoringVisitor();
        for (Declaration declaration : topLevelDeclarations) {
            declaration.accept(restoringVisitor, null);
        }
    }

    /**
     * Visitor that prepares declarations for the code size estimation.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class AdjustingVisitor extends ExceptionVisitor<Void, Void> {
        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            final String uniqueName = DeclaratorUtils.getUniqueName(
                    declaration.getDeclarator()).get();
            final LinkedList<TypeElement> originalSpecifiers = declaration.getModifiers();
            final SubstituteSupplier substituteSupplier = new SubstituteSupplier(originalSpecifiers,
                    !astPreservationData.containsKey(declaration));

            adjuster.adjustFunctionDefinition(
                    Collections.unmodifiableList(originalSpecifiers),
                    substituteSupplier,
                    uniqueName,
                    declaration.getDeclaration()
            );

            if (substituteSupplier.copy.isPresent()) {
                declaration.setModifiers(substituteSupplier.copy.get());
                astPreservationData.put(declaration, new OriginalState(originalSpecifiers));
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
            if (declaration.getDeclarations().isEmpty()) {
                return null;
            } else if (declaration.getDeclarations().size() != 1) {
                throw new IllegalStateException("unseparated declarations encountered");
            }

            final VariableDecl variableDecl =
                    (VariableDecl) declaration.getDeclarations().getFirst();
            final Optional<NestedDeclarator> deepestNestedDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(variableDecl.getDeclarator().get());

            if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof FunctionDeclarator) {
                // Forward declaration of a function
                final String uniqueName = DeclaratorUtils.getUniqueName(
                        variableDecl.getDeclarator().get()).get();
                final LinkedList<TypeElement> originalSpecifiers = declaration.getModifiers();
                final SubstituteSupplier substituteSupplier = new SubstituteSupplier(
                        originalSpecifiers, !astPreservationData.containsKey(declaration));

                adjuster.adjustFunctionDeclaration(
                        Collections.unmodifiableList(originalSpecifiers),
                        substituteSupplier,
                        uniqueName,
                        (FunctionDeclaration) variableDecl.getDeclaration()
                );

                if (substituteSupplier.copy.isPresent()) {
                    declaration.setModifiers(substituteSupplier.copy.get());
                    astPreservationData.put(declaration, new OriginalState(originalSpecifiers));
                }
            } else if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof InterfaceRefDeclarator) {
                throw new RuntimeException("unexpected interface reference declarator");
            }

            return null;
        }
    }

    /**
     * Visitor that restores state of declarations.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class RestoringVisitor extends ExceptionVisitor<Void, Void> {
        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            if (astPreservationData.containsKey(declaration)) {
                declaration.setModifiers(astPreservationData.get(declaration).typeElements);
                astPreservationData.remove(declaration);
            }
            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            if (astPreservationData.containsKey(declaration)) {
                declaration.setModifiers(astPreservationData.get(declaration).typeElements);
                astPreservationData.remove(declaration);
            }
            return null;
        }
    }

    /**
     * Supplier of a list of type elements that can be safely modified by the
     * adjuster.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class SubstituteSupplier implements Supplier<List<TypeElement>> {
        private final LinkedList<TypeElement> currentSpecifiers;
        private final boolean copyNecessary;
        private Optional<LinkedList<TypeElement>> copy;

        private SubstituteSupplier(LinkedList<TypeElement> currentSpecifiers,
                    boolean copyNecessary) {
            checkNotNull(currentSpecifiers, "current specifiers cannot be null");
            this.currentSpecifiers = currentSpecifiers;
            this.copyNecessary = copyNecessary;
            this.copy = Optional.absent();
        }

        @Override
        public List<TypeElement> get() {
            if (copy.isPresent()) {
                return copy.get();
            } else if (!copyNecessary) {
                return currentSpecifiers;
            } else {
                copy = Optional.of(AstUtils.deepCopyNodes(currentSpecifiers,
                        true, Optional.<Map<Node, Node>>absent()));
                return copy.get();
            }
        }
    }

    /**
     * Helper class whose object carry information about the original state of
     * an AST declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class OriginalState {
        private final LinkedList<TypeElement> typeElements;

        private OriginalState(LinkedList<TypeElement> typeElements) {
            checkNotNull(typeElements, "type elements cannot be null");
            this.typeElements = typeElements;
        }
    }
}
