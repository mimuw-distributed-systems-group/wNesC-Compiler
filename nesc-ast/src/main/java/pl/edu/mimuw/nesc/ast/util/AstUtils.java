package pl.edu.mimuw.nesc.ast.util;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.common.util.list.Lists;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <h1>Locations</h1>
 * <p>Methods for extracting locations are provided. They are
 * useful especially when one want to retrieve start/end location of some
 * language construct but the corresponding production consists of
 * several tokens list that are likely to be empty or some tokens are
 * optional.</p>
 *
 * <h1>Types</h1>
 * <p>There is a method for retrieving types from a list of declarations.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AstUtils {
    /**
     * <p>Predicate of an expression that is fulfilled if and only if the
     * expression is an initializer list or an initialization designation inside
     * an initializer list.</p>
     */
    public static final Predicate<Expression> IS_INITIALIZER = new Predicate<Expression>() {
        @Override
        public boolean apply(Expression expr) {
            checkNotNull(expr, "the expression cannot be null");
            return expr instanceof InitList || expr instanceof InitSpecific;
        }
    };

    /**
     * <p>Predicate that is fulfilled if and only if the type element is a core
     * type element (e.g. an attribute is not a core type element.</p>
     *
     * @see CoreTypeElementVisitor
     */
    public static final Predicate<TypeElement> IS_CORE_TYPE_ELEMENT = new Predicate<TypeElement>() {
        /**
         * Visitor that checks if the element is a core type element.
         */
        private final CoreTypeElementVisitor coreTypeElementVisitor = new CoreTypeElementVisitor();

        @Override
        public boolean apply(TypeElement typeElement) {
            checkNotNull(typeElement, "type element cannot be null");
            return typeElement.accept(coreTypeElementVisitor, null);
        }
    };

    /**
     * Returns start location of nodes in the list.
     *
     * @param nodesList nodes list
     * @param <T>       type of nodes
     * @return <code>absent</code> of start location of the first token
     */
    public static <T extends Node> Optional<Location> getStartLocation(LinkedList<T> nodesList) {
        if (nodesList.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(nodesList.getFirst().getLocation());
    }

    /**
     * <p>Return the minimum start location among all given node lists or
     * returns <code>absent</code>, when all lists are empty.</p>
     * <p>Lists are search from the first to the last one.</p>
     *
     * @param nodesList1 first nodes list
     * @param nodesList2 second nodes list
     * @param <T>        type of nodes list
     * @param <V>        type of nodes list
     * @return <code>absent</code> of start location of the first token
     */
    public static <T extends Node, V extends Node> Optional<Location> getStartLocation(
            LinkedList<T> nodesList1, LinkedList<V> nodesList2) {
        final Optional<Location> startLocation1 = getStartLocation(nodesList1);
        if (startLocation1.isPresent()) {
            return startLocation1;
        }
        final Optional<Location> startLocation2 = getStartLocation(nodesList2);
        if (startLocation2.isPresent()) {
            return startLocation2;
        }
        return Optional.absent();
    }

    /**
     * <p>Return the minimum start location in given node list or
     * returns the specified location, when the list is empty.</p>
     *
     * @param location  the location of the last token in production,
     *                  that is guaranteed to appear
     * @param nodesList nodes list
     * @param <T>       type of nodes list
     * @return start location
     */
    public static <T extends Node> Location getStartLocation(Location location, LinkedList<T> nodesList) {
        return getStartLocation(location, nodesList, Lists.<Node>newList());
    }

    /**
     * <p>Return the minimum start location among all given node lists or
     * returns the specified location, when all lists are empty.</p>
     * <p>Lists are search from the first to the last one.</p>
     *
     * @param location   the location of the last token in production,
     *                   that is guaranteed to appear
     * @param nodesList1 first nodes list
     * @param nodesList2 second nodes list
     * @param <T>        type of nodes list
     * @param <V>        type of nodes list
     * @return start location
     */
    public static <T extends Node, V extends Node> Location getStartLocation(Location location,
                                                                             LinkedList<T> nodesList1,
                                                                             LinkedList<V> nodesList2) {
        final Optional<Location> startLocation1 = getStartLocation(nodesList1);
        if (startLocation1.isPresent()) {
            return startLocation1.get();
        }
        final Optional<Location> startLocation2 = getStartLocation(nodesList2);
        if (startLocation2.isPresent()) {
            return startLocation2.get();
        }
        return location;
    }

    /**
     * <p>Returns end location of the last element of AST nodes list.</p>
     *
     * @param nodesList nodes list
     * @param <T>       type of nodes
     * @return <code>absent</code> when list is empty,
     * otherwise the end location of the last element of the list
     */
    public static <T extends Node> Optional<Location> getEndLocation(LinkedList<T> nodesList) {
        if (nodesList.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(nodesList.getLast().getEndLocation());
    }

    /**
     * <p>Return the maximum end location among all specified nodes list or
     * returns <code>absent</code>, when all lists are empty.</p>
     *
     * @param nodesList1 first nodes list
     * @param nodesList2 second nodes list
     * @param <T>        type of nodes list
     * @param <V>        type of nodes list
     * @return the maximum end location
     */
    public static <T extends Node, V extends Node> Optional<Location> getEndLocation(
            LinkedList<T> nodesList1, LinkedList<V> nodesList2) {
        final Optional<Location> endLocation2 = getEndLocation(nodesList2);
        if (endLocation2.isPresent()) {
            return endLocation2;
        }
        final Optional<Location> endLocation1 = getEndLocation(nodesList1);
        if (endLocation1.isPresent()) {
            return endLocation1;
        }
        return Optional.absent();
    }

    /**
     * Returns the maximum of the two end locations.
     *
     * @param endLocation         end location
     * @param endLocationOptional optional end location
     * @return the maximum of the two end locations
     */
    public static Location getEndLocation(Location endLocation, Optional<Location> endLocationOptional) {
        if (endLocationOptional.isPresent()) {
            return endLocationOptional.get();
        }
        return endLocation;
    }

    /**
     * <p>Return the maximum end location among given node lists or
     * returns the specified location, when all lists are empty.</p>
     *
     * @param location  the location of the last token in production,
     *                  that is guaranteed to appear
     * @param nodesList first nodes list
     * @param <T>       type of nodes list
     * @return the maximum end location
     */
    public static <T extends Node> Location getEndLocation(Location location, LinkedList<T> nodesList) {
        return getEndLocation(location, nodesList, Lists.<Node>newList(), Lists.<Node>newList());
    }

    /**
     * <p>Return the maximum end location among all specified nodes list or
     * returns the specified location, when all lists are empty.</p>
     *
     * @param location   the location of the last token in production,
     *                   that is guaranteed to appear
     * @param nodesList1 first nodes list
     * @param nodesList2 second nodes list
     * @param <T>        type of nodes list
     * @param <V>        type of nodes list
     * @return the maximum end location
     */
    public static <T extends Node, V extends Node> Location getEndLocation(
            Location location, LinkedList<T> nodesList1, LinkedList<V> nodesList2) {
        return getEndLocation(location, nodesList1, nodesList2, Lists.<Node>newList());
    }

    /**
     * <p>Return the maximum end location among all specified nodes list or
     * returns the specified location, when all lists are empty.</p>
     *
     * @param location   the location of the last token in production,
     *                   that is guaranteed to appear
     * @param nodesList1 first nodes list
     * @param nodesList2 second nodes list
     * @param nodesList3 last nodes list
     * @param <T>        type of nodes list
     * @param <V>        type of nodes list
     * @param <U>        type of nodes list
     * @return the maximum end location
     */
    public static <T extends Node, V extends Node, U extends Node> Location getEndLocation(
            Location location, LinkedList<T> nodesList1, LinkedList<V> nodesList2, LinkedList<U> nodesList3) {
        final Optional<Location> endLocation3 = getEndLocation(nodesList3);
        if (endLocation3.isPresent()) {
            return endLocation3.get();
        }
        final Optional<Location> endLocation2 = getEndLocation(nodesList2);
        if (endLocation2.isPresent()) {
            return endLocation2.get();
        }
        final Optional<Location> endLocation1 = getEndLocation(nodesList1);
        if (endLocation1.isPresent()) {
            return endLocation1.get();
        }
        return location;
    }

    /**
     * Creates {@link pl.edu.mimuw.nesc.ast.gen.Word} instance.
     *
     * @param startLocation start location
     * @param endLocation   end location
     * @param name          value of word
     * @return word instance
     */
    public static Word makeWord(Location startLocation, Location endLocation, String name) {
        final Word word = new Word(startLocation, name);
        word.setEndLocation(endLocation);
        return word;
    }

    /**
     * Returns a newly created linked list that contains attributes from the
     * given type elements list and the given attributes list.
     *
     * @param typeElements Type elements that potentially contains some
     *                     attributes that will be on the returned list.
     * @param attributes List with attributes that will be in the returned list.
     * @return Newly created list with attributes from the type elements first
     *         and then the attributes from the second list.
     */
    public static LinkedList<Attribute> joinAttributes(LinkedList<TypeElement> typeElements,
            LinkedList<Attribute> attributes) {
        final LinkedList<Attribute> allAttributes = new LinkedList<>();

        FluentIterable.from(typeElements)
                .filter(Attribute.class)
                .copyInto(allAttributes);
        allAttributes.addAll(attributes);

        return allAttributes;
    }

    /**
     * Get a string that is the value of given AST node.
     *
     * @param strings AST node with strings.
     * @return String that is the result of concatenation of all strings in
     *         given AST node in proper order.
     */
    public static String concatenateStrings(StringAst strings) {
        checkNotNull(strings, "string AST cannot be null");

        final StringBuilder builder = new StringBuilder();
        for (StringCst stringCst : strings.getStrings()) {
            builder.append(stringCst.getString());
        }

        return builder.toString();
    }

    /**
     * Extracts type from each declaration from the given list. The method can
     * be used only if all declarations from the list are <code>DataDecl</code>
     * objects and each such object contains exactly one declaration that is
     * a <code>VariableDecl</code> object. The list can also contain
     * <code>ErrorDecl</code> objects - such objects are ignored.
     *
     * @param declarations List of declarations to extract types from.
     * @return Immutable list with types from the given declarations (in proper
     *         order).
     * @throws IllegalArgumentException Declarations from the given list don't
     *                                  fulfill depicted requirements.
     */
    public static ImmutableList<Optional<Type>> getTypes(List<Declaration> declarations) {
        final ImmutableList.Builder<Optional<Type>> typesBuilder = ImmutableList.builder();

        for (Declaration declaration : declarations) {
            if (declaration instanceof ErrorDecl) {
                continue;
            }

            checkArgument(declaration instanceof DataDecl, "unexpected outer declaration class '%s'",
                    declaration.getClass());

            final DataDecl dataDecl = (DataDecl) declaration;
            final LinkedList<Declaration> dataDeclDeclarations = dataDecl.getDeclarations();
            checkArgument(dataDeclDeclarations.size() == 1, "unexpected declarations count %d", dataDeclDeclarations.size());

            final Declaration innerDeclaration = dataDeclDeclarations.getFirst();
            checkArgument(innerDeclaration instanceof VariableDecl, "unexpected inner declaration class '%s'",
                    innerDeclaration.getClass());

            final VariableDecl variableDecl = (VariableDecl) innerDeclaration;
            typesBuilder.add(variableDecl.getType());
        }

        return typesBuilder.build();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Node> LinkedList<T> deepCopyNodes(LinkedList<T> toCopy,
            boolean skipConstantFunCalls) {
        final LinkedList<T> copy = new LinkedList<>();

        for (T node : toCopy) {
            copy.add((T) node.deepCopy(skipConstantFunCalls));
        }

        return copy;
    }

    public static <T extends Node> Optional<LinkedList<T>> deepCopyNodes(Optional<LinkedList<T>> toCopy,
            boolean skipConstantFunCalls) {
        return toCopy.isPresent()
                ? Optional.of(deepCopyNodes(toCopy.get(), skipConstantFunCalls))
                : Optional.<LinkedList<T>>absent();
    }

    /**
     * Create an AST node that represents the return type of the function with
     * given definition.
     *
     * @param functionDecl AST node of the definition of a function to extract
     *                     the return type from.
     * @return Newly created AST node that represents the return type of the
     *         function with given definition.
     */
    public static AstType extractReturnType(FunctionDecl functionDecl) {
        checkNotNull(functionDecl, "function definition AST node cannot be null");

        // Type elements

        final LinkedList<TypeElement> typeElements = new LinkedList<>();
        for (TypeElement typeElement : functionDecl.getModifiers()) {
            if (IS_CORE_TYPE_ELEMENT.apply(typeElement)) {
                typeElements.add(typeElement.deepCopy(true));
            }
        }

        // Declarator

        final Optional<Declarator> typeDeclarator;
        if (functionDecl.getDeclarator() instanceof FunctionDeclarator) {
            typeDeclarator = Optional.absent();
        } else {
            NestedDeclarator firstDeclarator = (NestedDeclarator) functionDecl.getDeclarator();
            NestedDeclarator secondDeclarator = (NestedDeclarator) firstDeclarator.getDeclarator().get();

            while (!(secondDeclarator.getDeclarator().get() instanceof IdentifierDeclarator)
                    && !(secondDeclarator.getDeclarator().get() instanceof InterfaceRefDeclarator)) {
                firstDeclarator = secondDeclarator;
                secondDeclarator = (NestedDeclarator) secondDeclarator.getDeclarator().get();
            }

            checkArgument(secondDeclarator instanceof FunctionDeclarator,
                    "unexpected type of a nested declarator");

            firstDeclarator.setDeclarator(Optional.<Declarator>absent());
            typeDeclarator = Optional.of(functionDecl.getDeclarator().deepCopy(true));
            firstDeclarator.setDeclarator(Optional.<Declarator>of(secondDeclarator));
        }

        return new AstType(
                Location.getDummyLocation(),
                typeDeclarator,
                typeElements
        );
    }

    /**
     * Check if the return type of the given function is purely defined by the
     * type elements.
     *
     * @param functionDecl AST node of a function definition.
     * @return <code>true</code> if and only if the declarator has an impact on
     *         the return type of the given function.
     */
    public static boolean declaratorAffectsReturnType(FunctionDecl functionDecl) {
        checkNotNull(functionDecl, "function AST node cannot be null");

        if (functionDecl.getDeclarator() instanceof FunctionDeclarator) {
            return false;
        }

        NestedDeclarator firstDeclarator = (NestedDeclarator) functionDecl.getDeclarator();
        NestedDeclarator secondDeclarator = (NestedDeclarator) firstDeclarator.getDeclarator().get();

        while (!(secondDeclarator.getDeclarator().get() instanceof IdentifierDeclarator)
                && !(secondDeclarator.getDeclarator().get() instanceof InterfaceRefDeclarator)) {
            if (firstDeclarator instanceof ArrayDeclarator || firstDeclarator instanceof FunctionDeclarator
                    || firstDeclarator instanceof PointerDeclarator) {
                return true;
            }

            firstDeclarator = secondDeclarator;
            secondDeclarator = (NestedDeclarator) secondDeclarator.getDeclarator().get();
        }

        return false;
    }

    /**
     * Create an empty compound statement that may be used, i.e. as an empty
     * function body.
     *
     * @return Newly created compound statement without declarations or
     *         statements (empty lists are used).
     */
    public static CompoundStmt newEmptyCompoundStmt() {
        return new CompoundStmt(
                Location.getDummyLocation(),
                Lists.<IdLabel>newList(),
                Lists.<Declaration>newList(),
                Lists.<Statement>newList()
        );
    }

    private AstUtils() {
    }

    /**
     * <p>Visitor that returns <code>true</code> if it visits a type element that
     * is essential to determine the type, e.g. it filters the attributes type
     * elements.</p>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class CoreTypeElementVisitor extends ExceptionVisitor<Boolean, Void> {
        @Override
        public Boolean visitTypeofType(TypeofType typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitTypeofExpr(TypeofExpr typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitTypename(Typename typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitComponentTyperef(ComponentTyperef typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitAttributeRef(AttributeRef typeElement, Void arg) {
            return false;
        }

        @Override
        public Boolean visitStructRef(StructRef typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitNxStructRef(NxStructRef typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitEnumRef(EnumRef typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitUnionRef(UnionRef typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitNxUnionRef(NxUnionRef typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitRid(Rid typeElement, Void arg) {
            switch (typeElement.getId()) {
                case INT:
                case SHORT:
                case UNSIGNED:
                case SIGNED:
                case CHAR:
                case DOUBLE:
                case FLOAT:
                case LONG:
                case VOID:
                case COMPLEX:
                case CONST:
                case VOLATILE:
                case RESTRICT:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public Boolean visitQualifier(Qualifier typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitAttribute(Attribute attribute, Void arg) {
            return false;
        }

        @Override
        public Boolean visitGccAttribute(GccAttribute attribute, Void arg) {
            return false;
        }

        @Override
        public Boolean visitTargetAttribute(TargetAttribute attribute, Void arg) {
            return false;
        }

        @Override
        public Boolean visitNescAttribute(NescAttribute attribute, Void arg) {
            return false;
        }
    }
}
