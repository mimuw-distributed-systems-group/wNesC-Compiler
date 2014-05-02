package pl.edu.mimuw.nesc.semantic.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.declaration.nesc.InterfaceDeclaration;
import pl.edu.mimuw.nesc.declaration.nesc.NescDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.issue.NescIssue;

import java.util.LinkedList;

import static java.lang.String.format;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
final class SpecificationBuilder extends SemanticBase {

    private final Component component;

    public SpecificationBuilder(NescEntityEnvironment nescEnvironment,
                                ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                                Component component) {
        super(nescEnvironment, issuesMultimapBuilder);
        this.component = component;
    }

    public void build() {
        final SpecificationVisitor specVisitor = new SpecificationVisitor();
        specVisitor.visitDeclarations(component.getDeclarations());
    }

    private final class SpecificationVisitor extends ExceptionVisitor<Void, Void> {

        public void visitDeclarations(LinkedList<Declaration> declarations) {
            for (Declaration declaration : declarations) {
                if (declaration == null) {
                    continue;
                }
                declaration.accept(this, null);
            }
        }

        public Void visitErrorDecl(ErrorDecl declaration, Void arg) {
            /* ignore */
            return null;
        }

        public Void visitRequiresInterface(RequiresInterface elem, Void arg) {
            final RequiresProvidesVisitor rpVisitor = new RequiresProvidesVisitor(false);
            rpVisitor.visitDeclarations(elem.getDeclarations());
            return null;
        }

        public Void visitProvidesInterface(ProvidesInterface elem, Void arg) {
            final RequiresProvidesVisitor rpVisitor = new RequiresProvidesVisitor(true);
            rpVisitor.visitDeclarations(elem.getDeclarations());
            return null;
        }

        // TODO: bare event/command

        /*
         * Occurrences of other declarations should be reported as errors.
         */

    }

    private final class RequiresProvidesVisitor extends ExceptionVisitor<Void, Void> {

        private final boolean provides;

        private RequiresProvidesVisitor(boolean provides) {
            this.provides = provides;
        }

        public void visitDeclarations(LinkedList<Declaration> declarations) {
            for (Declaration declaration : declarations) {
                if (declaration == null) {
                    continue;
                }
                declaration.accept(this, null);
            }
        }

        @Override
        public Void visitErrorDecl(ErrorDecl errorDecl, Void arg) {
            // ignore
            return null;
        }

        @Override
        public Void visitInterfaceRef(InterfaceRef ref, Void arg) {
            final String name = ref.getAlias().isPresent() ? ref.getAlias().get().getName() : ref.getName().getName();
            final InterfaceRefDeclaration refDeclaration =
                    (InterfaceRefDeclaration) component.getSpecificationEnvironment().getObjects().get(name).get();


            final Word ifaceName = ref.getName();
            final Optional<? extends NescDeclaration> declaration = nescEnvironment.get(ifaceName.getName());
            if (!declaration.isPresent()) {
                errorHelper.error(ifaceName.getLocation(), Optional.of(ifaceName.getEndLocation()),
                        format("unknown interface '%s'", ifaceName.getName()));
                refDeclaration.setIfaceDeclaration(Optional.<InterfaceDeclaration>absent());
                return null;
            }
            if (!(declaration.get() instanceof InterfaceDeclaration)) {
                errorHelper.error(ifaceName.getLocation(), Optional.of(ifaceName.getEndLocation()),
                        format("'%s' is not interface", ifaceName.getName()));
                refDeclaration.setIfaceDeclaration(Optional.<InterfaceDeclaration>absent());
                return null;
            }
            final InterfaceDeclaration ifaceDeclaration = (InterfaceDeclaration) declaration.get();

            refDeclaration.setProvides(provides);
            refDeclaration.setIfaceDeclaration(Optional.of(ifaceDeclaration));

            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl dataDecl, Void arg) {
            // TODO: bare event/command
            return null;
        }

    }
}
