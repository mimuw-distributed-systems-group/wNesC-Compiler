package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.IntegerCstKind;
import pl.edu.mimuw.nesc.ast.IntegerCstSuffix;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.NescCallKind;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.type.IntType;
import pl.edu.mimuw.nesc.type.Type;
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
    public static <T extends Node> LinkedList<T> deepCopyNodes(List<T> toCopy, boolean skipConstantFunCalls,
            Optional<Map<Node, Node>> nodesMap) {
        final LinkedList<T> copy = new LinkedList<>();

        for (T node : toCopy) {
            copy.add((T) node.deepCopy(skipConstantFunCalls, nodesMap));
        }

        return copy;
    }

    public static <T extends Node> Optional<LinkedList<T>> deepCopyNodes(Optional<? extends List<T>> toCopy,
            boolean skipConstantFunCalls, Optional<Map<Node, Node>> nodesMap) {
        return toCopy.isPresent()
                ? Optional.of(deepCopyNodes(toCopy.get(), skipConstantFunCalls, nodesMap))
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

    /**
     * Creates an identifier with name and unique name set as the parameter and
     * fields <code>isGenericReference</code> and
     * <code>refsDeclInThisNescEntity</code> set to <code>false</code>.
     *
     * @param name Name and unique name of the created identifier.
     * @return Newly created identifier with fields properly set.
     */
    public static Identifier newIdentifier(String name) {
        return newIdentifier(name, name, false, false);
    }

    /**
     * Creates an identifier with fields set as specified by parameters.
     *
     * @param name Name to be set in the returned identifier.
     * @param uniqueName Unique name to be set in the returned identifier.
     * @param isGenericReference Value of the field with the same name to be set
     *                           in the identifier.
     * @param refsDeclInThisNescEntity Value of the field with the same name to
     *                                 be set in the identifier.
     * @return Newly created identifier with fields set the same as parameters.
     */
    public static Identifier newIdentifier(String name, String uniqueName, boolean isGenericReference,
            boolean refsDeclInThisNescEntity) {
        checkNotNull(name, "name cannot be null");
        checkNotNull(uniqueName, "unique name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");
        checkArgument(!uniqueName.isEmpty(), "unique name cannot be an empty string");

        final Identifier identifier = new Identifier(
                Location.getDummyLocation(),
                name
        );

        identifier.setUniqueName(Optional.of(uniqueName));
        identifier.setIsGenericReference(isGenericReference);
        identifier.setRefsDeclInThisNescEntity(refsDeclInThisNescEntity);

        return identifier;
    }

    /**
     * Creates a list of identifiers with strings from the given list used as
     * names and unique names of the identifiers.
     *
     * @param names List with names to use in identifiers.
     * @return Newly created list with identifiers specified by parameters.
     */
    public static LinkedList<Expression> newIdentifiersList(List<String> names) {
        checkNotNull(names, "names cannot be null");

        final LinkedList<Expression> identifiers = new LinkedList<>();
        for (String name : names) {
            identifiers.add(newIdentifier(name));
        }
        return identifiers;
    }

    /**
     * Create a list of type elements that consist only of the given rids.
     *
     * @param rids Rids to be contained on the returned list.
     * @return Newly created type elements list with given rids.
     */
    public static LinkedList<TypeElement> newRidsList(RID... rids) {
        final LinkedList<TypeElement> result = new LinkedList<>();
        for (RID rid : rids) {
            result.add(new Rid(Location.getDummyLocation(), rid));
        }
        return result;
    }

    /**
     * Create a simple variable declaration.
     *
     * @param name Name of the created variable.
     * @param uniqueName Unique name of the created variable.
     * @param isDeclaredInsideNescEntity Value for the declarator.
     * @param initializer Expression set as the initializer of the variable.
     * @param rids Elements that define the type of the variable.
     * @return Newly created declaration of a variable specified by parameters.
     */
    public static DataDecl newSimpleDeclaration(String name, String uniqueName, boolean isDeclaredInsideNescEntity,
            Optional<Expression> initializer, RID... rids) {
        return newSimpleDeclaration(name, uniqueName, isDeclaredInsideNescEntity, initializer,
                new AstType(Location.getDummyLocation(), Optional.<Declarator>absent(), newRidsList(rids)));
    }

    public static DataDecl newSimpleDeclaration(String name, String uniqueName, boolean isDeclaredInsideNescEntity,
            Optional<Expression> initializer, AstType astType) {
        // Declarator with variable name

        final IdentifierDeclarator identDeclarator = new IdentifierDeclarator(
                Location.getDummyLocation(),
                name
        );
        identDeclarator.setIsNestedInNescEntity(isDeclaredInsideNescEntity);
        identDeclarator.setUniqueName(Optional.of(uniqueName));

        // Declarator of the variable

        final Optional<NestedDeclarator> deepestNestedDeclarator =
                DeclaratorUtils.getDeepestNestedDeclarator(astType.getDeclarator());
        final Declarator declarator;
        if (deepestNestedDeclarator.isPresent()) {
            deepestNestedDeclarator.get().setDeclarator(Optional.<Declarator>of(identDeclarator));
            declarator = deepestNestedDeclarator.get();
        } else {
            declarator = identDeclarator;
        }

        // Variable declaration

        final VariableDecl variableDecl = new VariableDecl(
                Location.getDummyLocation(),
                Optional.of(declarator),
                Lists.<Attribute>newList(),
                Optional.<AsmStmt>absent()
        );
        variableDecl.setInitializer(initializer);

        return new DataDecl(
                Location.getDummyLocation(),
                astType.getQualifiers(),
                Lists.<Declaration>newList(variableDecl)
        );
    }

    /**
     * Creates a function call AST node.
     *
     * @param funName Name of the function identifier (use also as its unique
     *                name).
     * @param parametersNames Names of identifiers used as parameters for the
     *                        returned function call (the identifiers have the
     *                        same names and unique names).
     * @return Newly created function call specified by given parameters.
     */
    public static FunctionCall newNormalCall(String funName, List<String> parametersNames) {
        return new FunctionCall(
                Location.getDummyLocation(),
                newIdentifier(funName),
                newIdentifiersList(parametersNames),
                NescCallKind.NORMAL_CALL
        );
    }

    /**
     * Creates a function call AST node using the given name for the identifier
     * (that is created for the node) and directly the given list of parameters.
     *
     * @param funName Name to be used for the function identifier.
     * @param parameters List that will be used for parameters.
     * @return Newly created AST node of function call.
     */
    public static FunctionCall newNormalCall(String funName, LinkedList<Expression> parameters) {
        return new FunctionCall(
                Location.getDummyLocation(),
                newIdentifier(funName),
                parameters,
                NescCallKind.NORMAL_CALL
        );
    }

    /**
     * Creates an constant integer expression that evaluates to the given value.
     *
     * @param value Value of the integer constant expression to create.
     * @return Newly created integer constant expression that evaluates to the
     *         given value.
     */
    public static Expression newIntegerConstant(int value) {
        final boolean negation = value < 0;
        value = Math.abs(value);

        final IntegerCst constant = new IntegerCst(
                Location.getDummyLocation(),
                Integer.toString(value),
                Optional.of(BigInteger.valueOf(value)),
                IntegerCstKind.DECIMAL,
                IntegerCstSuffix.NO_SUFFIX
        );
        constant.setType(Optional.<Type>of(new IntType()));

        final Expression result;

        if (negation) {
            result = new UnaryMinus(Location.getDummyLocation(), constant);
            result.setType(Optional.<Type>of(new IntType()));
        } else {
            result = constant;
        }

        return result;
    }

    /**
     * Creates a new return statement that causes returning of the given
     * constant.
     *
     * @param value Integer constant to be returned by the created statement.
     * @return Newly created statement that causes returning the given constant
     *         value.
     */
    public static ReturnStmt newReturnStmt(int value) {
        return new ReturnStmt(
                Location.getDummyLocation(),
                Optional.of(newIntegerConstant(value))
        );
    }

    /**
     * Method for quickly creating a return statement that causes returning
     * value of a variable with given name. The name is also used as the unique
     * name of the created identifier.
     *
     * @param name Name and unique name of the identifier in the created
     *             'return' statement.
     * @return Newly created return statement that causes returning the value of
     *         the variable with name and unique name as given parameter.
     */
    public static ReturnStmt newReturnStmt(String name) {
        return newReturnStmt(name, name, false, false);
    }

    /**
     * Creates a return statement that causes returning an identifier specified
     * by given parameters.
     *
     * @param identifierName Name to set in the identifier.
     * @param identifierUniqueName Unique name to set in the identifier.
     * @param isGenericReference Value to set as 'isGenericReference' in the
     *                           identifier.
     * @param refsDeclInNescEntity Value to set as 'refsDeclInNescEntity' in
     *                             the identifier.
     * @return Newly created return statement that causes returning an
     *         identifier specified by given parameters.
     */
    public static ReturnStmt newReturnStmt(String identifierName, String identifierUniqueName,
            boolean isGenericReference, boolean refsDeclInNescEntity) {
        final Identifier identifier = newIdentifier(identifierName, identifierUniqueName,
                isGenericReference, refsDeclInNescEntity);
        return new ReturnStmt(
                Location.getDummyLocation(),
                Optional.<Expression>of(identifier)
        );
    }

    /**
     * Create a logical and expression that contains as arguments expressions
     * from given list.
     *
     * @param expressions List with expressions that will be elements of
     *                    the returned logical and expression.
     * @return Newly created logical and expression that contains expressions
     *         from the given list as elements.
     */
    public static Expression newLogicalAnd(List<? extends Expression> expressions) {
        checkNotNull(expressions, "expressions list cannot be null");
        checkArgument(!expressions.isEmpty(), "expressions list cannot be empty");

        if (expressions.size() == 1) {
            return expressions.get(0);
        }

        Andand accumulator = new Andand(
                Location.getDummyLocation(),
                expressions.get(expressions.size() - 2),
                expressions.get(expressions.size() - 1)
        );

        final ListIterator<? extends Expression> exprsIt =
                expressions.listIterator(expressions.size() - 2);

        while (exprsIt.hasPrevious()) {
            accumulator = new Andand(
                    Location.getDummyLocation(),
                    exprsIt.previous(),
                    accumulator
            );
        }

        return accumulator;
    }

    /**
     * <p>Creates a list of '==' expressions from lists of left and right sides
     * for the operator. The returned list has the same size as the given lists.
     * </p>
     *
     * <p>Example:
     * <code>newEqualsExpressions([2 + x, 10 * y], [10, 20])</code>
     * returns <code>[2 + x == 10, 10 * y == 20]</code>.</p>
     *
     * @param leftSides List with left sides for the '==' operator.
     * @param rightSides List with right sides for the '==' operator.
     * @return Newly created list with equality expressions.
     */
    public static LinkedList<Expression> zipWithEq(List<? extends Expression> leftSides,
            List<? extends Expression> rightSides) {
        checkNotNull(leftSides, "left sides cannot be null");
        checkNotNull(rightSides, "right sides cannot be null");
        checkArgument(leftSides.size() == rightSides.size(), "sizes of the lists cannot differ");
        checkArgument(!leftSides.isEmpty(), "lists cannot be empty");

        final LinkedList<Expression> result = new LinkedList<>();
        final Iterator<? extends Expression> leftExprIt = leftSides.iterator();
        final Iterator<? extends Expression> rightExprIt = rightSides.iterator();

        while (leftExprIt.hasNext()) {
            result.add(new Eq(
                    Location.getDummyLocation(),
                    leftExprIt.next(),
                    rightExprIt.next()
            ));
        }

        return result;
    }

    /**
     * Creates a forward declaration for the definition of the given function.
     *
     * @param functionDecl Definition of a function.
     * @return Newly created forward declaration of the given function.
     */
    public static DataDecl createForwardDeclaration(FunctionDecl functionDecl) {
        checkNotNull(functionDecl, "function definition cannot be null");

        final VariableDecl variableDecl = new VariableDecl(
                Location.getDummyLocation(),
                Optional.of(functionDecl.getDeclarator().deepCopy(true)),
                Lists.<Attribute>newList(),
                Optional.<AsmStmt>absent()
        );
        variableDecl.setInitializer(Optional.<Expression>absent());

        return new DataDecl(
                Location.getDummyLocation(),
                deepCopyNodes(functionDecl.getModifiers(), true, Optional.<Map<Node, Node>>absent()),
                Lists.<Declaration>newList(variableDecl)
        );
    }

    /**
     * Check if the given declaration is a definition of a NesC attribute.
     *
     * @param dataDecl Declaration to check.
     * @return <code>true</code> if and only if the given declaration is a NesC
     *         attribute definition (in such case the attribute is the only
     *         element the declaration introduces).
     */
    public static boolean isNescAttributeDefinition(DataDecl dataDecl) {
        checkNotNull(dataDecl, "declaration cannot be null");

        for (TypeElement typeElement : dataDecl.getModifiers()) {
            if (typeElement instanceof AttributeRef) {
                return true;
            }
        }

        return false;
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
