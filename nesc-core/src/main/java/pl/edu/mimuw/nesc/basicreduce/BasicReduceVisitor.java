package pl.edu.mimuw.nesc.basicreduce;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.AttributeRef;
import pl.edu.mimuw.nesc.ast.gen.ComponentTyperef;
import pl.edu.mimuw.nesc.ast.gen.EnumRef;
import pl.edu.mimuw.nesc.ast.gen.Enumerator;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.IdentifierDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NullVisitor;
import pl.edu.mimuw.nesc.ast.gen.NxStructRef;
import pl.edu.mimuw.nesc.ast.gen.NxUnionRef;
import pl.edu.mimuw.nesc.ast.gen.StructRef;
import pl.edu.mimuw.nesc.ast.gen.TypeParmDecl;
import pl.edu.mimuw.nesc.ast.gen.Typename;
import pl.edu.mimuw.nesc.ast.gen.UnionRef;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;

import static com.google.common.base.Preconditions.checkState;

/**
 * <p>A visitor for traversing AST nodes that is responsible for performing
 * basic reduce actions.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see BasicReduceExecutor
 */
final class BasicReduceVisitor extends NullVisitor<Void, Void> {
    /**
     * Set with all not mangled names that are to be located in the global scope
     * at the end of compiling.
     */
    private final ImmutableSet<String> globalNames;

    /**
     * Keys in the map are unique names and values are their final names in the
     * end of the compiling process. Initially this is mapping of unique names
     * of global entities to their original names.
     */
    private final Map<String, String> uniqueNamesMap;

    /**
     * Name mangler used for generating new names if a global name (before
     * mangling) is the same as an unique name. It shall be the same object used
     * for performing the mangling in the traversed nodes.
     */
    private final NameMangler mangler;

    /**
     * Get a new builder that will build a basic reduce visitor.
     *
     * @return Newly created builder that will build a basic reduce visitor.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initializes this visitor with information provided by the builder.
     *
     * @param builder Builder with information necessary to initialize this
     *                visitor.
     */
    private BasicReduceVisitor(Builder builder) {
        this.globalNames = builder.buildGlobalNames();
        this.uniqueNamesMap = builder.buildUniqueNamesMap();
        this.mangler = builder.mangler;
    }

    @Override
    public Void visitEnumerator(Enumerator node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    @Override
    public Void visitTypename(Typename node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    @Override
    public Void visitComponentTyperef(ComponentTyperef node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    @Override
    public Void visitAttributeRef(AttributeRef node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    @Override
    public Void visitStructRef(StructRef node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    @Override
    public Void visitNxStructRef(NxStructRef node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    @Override
    public Void visitUnionRef(UnionRef node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    @Override
    public Void visitNxUnionRef(NxUnionRef node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    @Override
    public Void visitEnumRef(EnumRef node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    @Override
    public Void visitIdentifierDeclarator(IdentifierDeclarator node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    @Override
    public Void visitIdentifier(Identifier node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    @Override
    public Void visitTypeParmDecl(TypeParmDecl node, Void arg) {
        node.setUniqueName(getFinalUniqueName(node.getUniqueName()));
        return null;
    }

    private String getFinalUniqueName(String currentUniqueName) {
        final Optional<String> optFinalUniqueName = Optional.fromNullable(uniqueNamesMap.get(currentUniqueName));

        if (optFinalUniqueName.isPresent()) {
            /* Remove the mangling of the name that will be global or update
               a remangled name. */
            return optFinalUniqueName.get();
        } else if (globalNames.contains(currentUniqueName)) {
            // Change the unique name that has been mangled to a global name
            final String finalUniqueName = mangler.remangle(currentUniqueName);
            uniqueNamesMap.put(currentUniqueName, finalUniqueName);
            return finalUniqueName;
        }

        // The unique name stays the same
        return currentUniqueName;
    }

    private Optional<String> getFinalUniqueName(Optional<String> currentUniqueName) {
        return currentUniqueName.isPresent()
                ? Optional.of(getFinalUniqueName(currentUniqueName.get()))
                : currentUniqueName;
    }

    /**
     * Builder for the basic reduce visitor.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    static final class Builder {
        /**
         * Objects needed to build a basic reduce visitor.
         */
        private NameMangler mangler;
        private Map<String, String> globalNames = new HashMap<>();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Set the name mangler that will be used for generating needed unique
         * names. It shall guarantee that no global name added to this builder
         * will be repeated. In other words, the global names should be added as
         * forbidden to the mangler.
         *
         * @param nameMangler Name mangler to set.
         * @return <code>this</code>
         */
        public Builder nameMangler(NameMangler nameMangler) {
            this.mangler = nameMangler;
            return this;
        }

        /**
         * Adds mapping from unique names to corresponding global names.
         * Previously added mappings with the same keys as in the parameter are
         * overwritten.
         *
         * @param globalNames Map from unique names to global names.
         * @return <code>this</code>
         */
        public Builder putGlobalNames(Map<String, String> globalNames) {
            this.globalNames.putAll(globalNames);
            return this;
        }

        private void validate() {
            checkState(mangler != null, "mangler has not been set or is set to null");
            checkState(!globalNames.containsKey(null), "a mapping from null has been added");
            checkState(!globalNames.containsValue(null), "a mapping to a null value has been added");
            checkState(!globalNames.containsKey(""), "a mapping from an empty string has been added");
            checkState(!globalNames.containsValue(""), "a mapping to an empty string has been added");
        }

        public BasicReduceVisitor build() {
            validate();
            return new BasicReduceVisitor(this);
        }

        private ImmutableSet<String> buildGlobalNames() {
            return ImmutableSet.copyOf(globalNames.values());
        }

        private Map<String, String> buildUniqueNamesMap() {
            return new HashMap<>(globalNames);
        }
    }
}
