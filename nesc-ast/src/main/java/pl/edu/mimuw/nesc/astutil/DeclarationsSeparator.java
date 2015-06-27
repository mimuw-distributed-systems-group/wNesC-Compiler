package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.AttributeRef;
import pl.edu.mimuw.nesc.ast.gen.CompoundStmt;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FieldDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.NxStructRef;
import pl.edu.mimuw.nesc.ast.gen.NxUnionRef;
import pl.edu.mimuw.nesc.ast.gen.StructRef;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.UnionRef;
import pl.edu.mimuw.nesc.ast.gen.Visitor;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Object that separates declarations of multiple entities if they are
 * contained in a single C declaration. Declarations that are separated:</p>
 * <ul>
 *     <li>each {@link VariableDecl} contained in a top-level {@link DataDecl}
 *     is separated to its own <code>DataDecl</code></li>
 *     <li>if separating declarations of compound statements is turned on, each
 *     {@link VariableDecl} contained in a {@link DataDecl} from a compound
 *     statement is separated to its own <code>DataDecl</code></li>
 *     <li>if separating tag definitions is turned on, each {@link FieldDecl}
 *     contained in a {@link DataDecl} from a {@link TagRef} is separated to
 *     its own <code>DataDecl</code></li>
 * </ul>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class DeclarationsSeparator {
    /**
     * Name mangler used to create names for unnamed tags if it is necessary.
     */
    private NameMangler nameMangler;

    /**
     * Value that indicates if declarations from compound statements will be
     * separated.
     */
    private boolean separateBlockDeclarations;

    /**
     * Value that indicates if declarations of fields of structures and unions
     * will be separated.
     */
    private boolean separateTagDefinitions;

    /**
     * The instance of a top-level separating visitor that will be used by this
     * separator.
     */
    private final TopLevelSeparatingVisitor topLevelSeparatingVisitor;

    /**
     * The instance of deep separating visitor that will be used by this
     * separator.
     */
    private final DeepSeparatingVisitor deepSeparatingVisitor;

    /**
     * Initialize this declarations separator to use the given name mangler for
     * creating names of unnamed tags if it is necessary. Separating
     * declarations in compound statements and of fields in tag definitions is
     * turned on.
     *
     * @param nameMangler Name mangler to use for names creation.
     */
    public DeclarationsSeparator(NameMangler nameMangler) {
        checkNotNull(nameMangler, "name mangler cannot be null");
        this.nameMangler = nameMangler;
        this.separateTagDefinitions = this.separateBlockDeclarations = true;
        this.topLevelSeparatingVisitor = new TopLevelSeparatingVisitor();
        this.deepSeparatingVisitor = new DeepSeparatingVisitor();
    }

    /**
     * Separates declarations from the given iterable according to current
     * configuration. The order of declarations defined by the iterator of the
     * iterable is preserved in the returned list.
     *
     * @param declarations Iterable with declarations to separate.
     * @return Newly created list with declarations after separation.
     */
    public ImmutableList<Declaration> separate(Iterable<? extends Declaration> declarations) {
        checkNotNull(declarations, "declarations cannot be null");

        final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();

        for (Declaration declaration : declarations) {
            final Optional<LinkedList<Declaration>> separatedDecls =
                    declaration.accept(topLevelSeparatingVisitor, null);

            if (separatedDecls.isPresent()) {
                declarationsBuilder.addAll(separatedDecls.get());
            } else {
                declarationsBuilder.add(declaration);
            }
        }

        return declarationsBuilder.build();
    }

    /**
     * Separates declarations in the given data declaration if there are more
     * than one. Moreover, declarations in inner AST nodes are also separated
     * according to configuration of this separator.
     *
     * @param dataDecl Data declaration with multiple declarations to separate.
     * @return If the given data declaration object contains less than 2 inner
     *         declarations, then the object is absent. Otherwise, it is present
     *         and a list of equivalent declarations is returned (each object on
     *         the list is a data declaration object and each contains exactly
     *         one nested declaration).
     */
    public Optional<LinkedList<Declaration>> separate(DataDecl dataDecl) {
        checkNotNull(dataDecl, "data declaration cannot be null");

        deepSeparate(dataDecl);

        if (dataDecl.getDeclarations().size() < 2) {
            return Optional.absent();
        }

        // Prepare type elements

        AstUtils.nameTags(dataDecl.getModifiers(), nameMangler);
        final LinkedList<TypeElement> typeElements = AstUtils.deepCopyNodes(dataDecl.getModifiers(), true,
                Optional.<Map<Node, Node>>absent());
        AstUtils.undefineTags(typeElements);

        // Create list of declarations

        final Iterator<Declaration> innerDeclIt = dataDecl.getDeclarations().iterator();
        final LinkedList<Declaration> declarations = Lists.<Declaration>newList(dataDecl);
        dataDecl.setDeclarations(Lists.newList(innerDeclIt.next()));

        while (innerDeclIt.hasNext()) {
            declarations.add(new DataDecl(
                    Location.getDummyLocation(),
                    AstUtils.deepCopyNodes(typeElements, true, Optional.<Map<Node, Node>>absent()),
                    Lists.newList(innerDeclIt.next())
            ));
        }

        return Optional.of(declarations);
    }

    /**
     * Set the name mangler that will be used to create names if it will be
     * necessary.
     *
     * @param nameMangler Name mangler to set.
     */
    public void setNameMangler(NameMangler nameMangler) {
        checkNotNull(nameMangler, "name mangler cannot be null");
        this.nameMangler = nameMangler;
    }

    /**
     * Set what will be separated by subsequent calls to <code>separate</code>
     * methods.
     *
     * @param separateCompoundStmtDecls Value that specifies if declarations in
     *                                  compound statements will be separated.
     * @param separateTagDefinitions Value that specifies if field declarations
     *                               in tag definitions will be separated.
     */
    public void configure(boolean separateCompoundStmtDecls,
            boolean separateTagDefinitions) {
        this.separateBlockDeclarations = separateCompoundStmtDecls;
        this.separateTagDefinitions = separateTagDefinitions;
    }

    private void deepSeparate(Node node) {
        if (separateBlockDeclarations || separateTagDefinitions) {
            node.traverse(deepSeparatingVisitor, null);
        }
    }

    /**
     * Visitor for separation of top-level declarations.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class TopLevelSeparatingVisitor extends ExceptionVisitor<Optional<LinkedList<Declaration>>, Void> {
        @Override
        public Optional<LinkedList<Declaration>> visitFunctionDecl(FunctionDecl declaration, Void arg) {
            deepSeparate(declaration);
            return Optional.absent();
        }

        @Override
        public Optional<LinkedList<Declaration>> visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            final Optional<LinkedList<Declaration>> separatedDecl =
                    declaration.getDeclaration().accept(this, null);

            if (!separatedDecl.isPresent()) {
                return Optional.absent();
            } else {
                final LinkedList<Declaration> result = new LinkedList<>();
                for (Declaration sepDeclaration : separatedDecl.get()) {
                    result.add(new ExtensionDecl(Location.getDummyLocation(), sepDeclaration));
                }
                return Optional.of(result);
            }
        }

        @Override
        public Optional<LinkedList<Declaration>> visitDataDecl(DataDecl declaration, Void arg) {
            return separate(declaration);
        }
    }

    /**
     * Visitor that separates block declarations and declarations of fields in
     * tag definitions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class DeepSeparatingVisitor extends IdentityVisitor<Void> {
        @Override
        public Void visitCompoundStmt(CompoundStmt stmt, Void arg) {
            if (separateBlockDeclarations) {
                separateDeclarations(stmt.getDeclarations());
            }
            return null;
        }

        @Override
        public Void visitStructRef(StructRef tagRef, Void arg) {
            separateDeclarations(tagRef);
            return null;
        }

        @Override
        public Void visitAttributeRef(AttributeRef tagRef, Void arg) {
            separateDeclarations(tagRef);
            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef tagRef, Void arg) {
            separateDeclarations(tagRef);
            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef tagRef, Void arg) {
            separateDeclarations(tagRef);
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef tagRef, Void arg) {
            separateDeclarations(tagRef);
            return null;
        }

        private void separateDeclarations(TagRef tagRef) {
            if (separateTagDefinitions) {
                separateDeclarations(tagRef.getFields());
            }
        }

        private void separateDeclarations(LinkedList<Declaration> declarations) {
            if (declarations == null) {
                return;
            }

            final ListIterator<Declaration> declsIt = declarations.listIterator();

            while (declsIt.hasNext()) {
                final DataDecl declaration = (DataDecl) declsIt.next();
                final Optional<LinkedList<Declaration>> separatedDecls = separate(declaration);

                if (separatedDecls.isPresent()) {
                    final Iterator<Declaration> newDeclsIt = separatedDecls.get().iterator();
                    declsIt.set(newDeclsIt.next());

                    while (newDeclsIt.hasNext()) {
                        declsIt.add(newDeclsIt.next());
                    }
                }
            }
        }
    }
}
