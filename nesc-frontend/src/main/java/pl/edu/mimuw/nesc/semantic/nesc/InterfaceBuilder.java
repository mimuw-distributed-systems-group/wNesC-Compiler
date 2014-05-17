package pl.edu.mimuw.nesc.semantic.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.declaration.nesc.InterfaceDeclaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration.FunctionType;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.issue.NescIssue;

import java.util.LinkedList;

import static java.lang.String.format;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
final class InterfaceBuilder extends SemanticBase {

    private final NescEntityEnvironment nescEnvironment;
    private final Interface iface;
    private final InterfaceDeclaration ifaceDeclaration;

    public InterfaceBuilder(NescEntityEnvironment nescEnvironment,
                            ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                            Interface iface) {
        super(nescEnvironment, issuesMultimapBuilder);
        this.nescEnvironment = nescEnvironment;
        this.iface = iface;
        this.ifaceDeclaration = iface.getDeclaration();
    }

    public void build() {
        final Word name = iface.getName();

        if (!nescEnvironment.add(name.getName(), ifaceDeclaration)) {
            errorHelper.error(name.getLocation(), Optional.of(name.getEndLocation()),
                    format("redefinition of '%s'", name.getName()));
        }

        /* Check interface parameters. */
        // TODO

        /* Check interface declarations. */
        final InterfaceBodyVisitor bodyVisitor = new InterfaceBodyVisitor();
        bodyVisitor.visitDeclarations(iface.getDeclarations());
    }

    private final class InterfaceBodyVisitor extends ExceptionVisitor<Void, Void> {

        public void visitDeclarations(LinkedList<Declaration> declarations) {
            for (Declaration declaration : declarations) {
                if (declaration == null) {
                    /* null for empty statement and target def. */
                    continue;
                }
                declaration.accept(this, null);
            }
        }

        public Void visitErrorDecl(ErrorDecl declaration, Void arg) {
            /* ignore */
            return null;
        }

        public Void visitDataDecl(DataDecl declaration, Void arg) {
            final InterfaceDeclarationVisitor visitor = new InterfaceDeclarationVisitor(declaration.getModifiers());
            visitor.visitDeclarations(declaration.getDeclarations());
            return null;
        }
    }

    private final class InterfaceDeclarationVisitor extends ExceptionVisitor<Void, Void> {

        private final LinkedList<TypeElement> modifiers;

        private InterfaceDeclarationVisitor(LinkedList<TypeElement> modifiers) {
            this.modifiers = modifiers;
        }

        /*
         * Environment already contains declarations.
         */

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

        public Void visitVariableDecl(VariableDecl declaration, Void arg) {
            final Declarator declarator = declaration.getDeclarator().get();
            if (!(declarator instanceof FunctionDeclarator)) {
                errorHelper.error(declarator.getLocation(), Optional.of(declarator.getEndLocation()),
                        format("only commands and events can be defined in interfaces"));
                // TODO: make this declaration dummy
                return null;
            }
            final FunctionDeclarator funDeclarator = (FunctionDeclarator) declarator;
            final FunctionDeclaration funDeclaration = (FunctionDeclaration) declaration.getDeclaration();

            final FunctionType functionType = getFunctionType(modifiers);
            funDeclaration.setFunctionType(functionType);
            return null;
        }

        // FIXME: remove (almost the same code in TypeElementUtils)
        private FunctionType getFunctionType(LinkedList<TypeElement> qualifiers) {
            // FIXME: temporary solution, this kind of information should be
            // kept in type object
            for (TypeElement element : qualifiers) {
                if (element instanceof Rid) {
                    final Rid rid = (Rid) element;
                    if (rid.getId() == RID.COMMAND) {
                        return FunctionType.COMMAND;
                    }
                    if (rid.getId() == RID.EVENT) {
                        return FunctionType.EVENT;
                    }
                }
            }
            return FunctionType.NORMAL;
        }

        // TODO: all other declarations should be reported as errors
    }

}
