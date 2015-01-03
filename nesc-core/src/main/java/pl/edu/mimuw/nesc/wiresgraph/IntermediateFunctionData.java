package pl.edu.mimuw.nesc.wiresgraph;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Data of an intermediate function. It corresponds to a source or a node
 * with positive both in- and out-degrees in the wires graph.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IntermediateFunctionData extends EntityData {
    /**
     * Names of arguments of the intermediate function for this entity data.
     * Potential instance parameters are at the beginning of the list in the
     * proper order.
     */
    private final ImmutableList<String> parametersNames;

    /**
     * Names of parameters of the intermediate function that correspond to
     * the instance parameters of the command or event.
     */
    private final ImmutableList<String> instanceParametersNames;

    /**
     * Value indicating if the command or event does not return any value.
     */
    private final boolean returnsVoid;

    /**
     * AST node for the creation of implementation of the intermediate function.
     * It contains proper prototype but empty body.
     */
    private final FunctionDecl intermediateFunction;

    /**
     * Unique name of the function that is the default implementation of the
     * command or event.
     */
    private final Optional<String> defaultImplementationUniqueName;

    /**
     * Unique name of the function that returns whether the intermediate
     * function returns a valid result.
     */
    private Optional<String> validResultFunctionUniqueName = Optional.absent();

    /**
     * Saves the values from parameters in member fields.
     *
     * @param uniqueName Unique name of the intermediate function.
     * @param argumentsNames Names of arguments for the intermediate function.
     * @param instanceParametersCount Count of the initial parameters of the
     *                                intermediate function that correspond to
     *                                the instance parameters.
     * @param returnsVoid Value indicating if the function returns void.
     * @param intermediateFunTemplate AST node for the creation of intermediate
     *                                function.
     * @param defaultImplementationUniqueName Unique name of the function that
     *                                        is the default implementation of
     *                                        the command or event.
     * @throws NullPointerException One of the reference arguments is
     *                              <code>null</code>.
     * @throws IllegalArgumentException Unique name is an empty string.
     */
    IntermediateFunctionData(String uniqueName, ImmutableList<String> argumentsNames,
            int instanceParametersCount, boolean returnsVoid,
            FunctionDecl intermediateFunTemplate,
            Optional<String> defaultImplementationUniqueName) {
        super(uniqueName);  // throws if 'uniqueName' is null or empty

        checkNotNull(argumentsNames, "names of parameters cannot be null");
        checkNotNull(intermediateFunTemplate, "the template of intermediate function cannot be null");
        checkNotNull(defaultImplementationUniqueName, "the unique name of the default implementation cannot be null");
        checkArgument(!defaultImplementationUniqueName.isPresent() || !defaultImplementationUniqueName.get().isEmpty(),
                "the unique name of the default implementation cannot be an empty string");
        checkArgument(instanceParametersCount >= 0, "instance parameters count cannot be negative");

        this.parametersNames = argumentsNames;
        this.instanceParametersNames = argumentsNames.subList(0, instanceParametersCount);
        this.returnsVoid = returnsVoid;
        this.intermediateFunction = intermediateFunTemplate;
        this.defaultImplementationUniqueName = defaultImplementationUniqueName;
    }

    @Override
    public boolean isImplemented() {
        return false;
    }

    /**
     * Get the names of all parameters to the intermediate function (including
     * parameters that are added because of instance parameters).
     *
     * @return List with names of parameters.
     */
    public ImmutableList<String> getParametersNames() {
        return parametersNames;
    }

    /**
     * List with names of initial parameters of the intermediate function that
     * correspond to instance parameters of the command or event.
     *
     * @return List with names of all instance parameters.
     */
    public ImmutableList<String> getInstanceParametersNames() {
        return instanceParametersNames;
    }

    /**
     * Check if the intermediate function returns <code>void</code>.
     *
     * @return <code>true</code> if and only if the function returns nothing.
     */
    public boolean returnsVoid() {
        return returnsVoid;
    }

    /**
     * Get the AST node for creation of the body of the intermediate function.
     *
     * @return AST node for construction of the intermediate function.
     */
    public FunctionDecl getIntermediateFunction() {
        return intermediateFunction;
    }

    /**
     * Get the unique name of the function that is the default implementation of
     * the command or event.
     *
     * @return Unique name of the default implementation of the command or
     *         event. The object is absent if and only if there is no default
     *         implementation for the command or event.
     */
    public Optional<String> getDefaultImplementationUniqueName() {
        return defaultImplementationUniqueName;
    }

    /**
     * Get the unique name of the function that checks if the result of the
     * intermediate function associated with this node is valid.
     *
     * @return Unique name of the function that checks if the result of the
     *         intermediate function associated with this node is valid.
     */
    public Optional<String> getValidResultFunctionUniqueName() {
        return validResultFunctionUniqueName;
    }

    /**
     * Set the unique name of the function that returns if the intermediate
     * function associated with the node is valid.
     *
     * @param uniqueName Unique name to set.
     * @throws NullPointerException Unique name is <code>null</code>.
     * @throws IllegalArgumentException Unique name is an empty string.
     * @throws IllegalStateException Unique name has been already set.
     */
    public void setValidResultFunctionUniqueName(String uniqueName) {
        checkNotNull(uniqueName, "unique name of the function cannot be null");
        checkArgument(!uniqueName.isEmpty(), "unique name of the function cannot be an empty string");
        checkState(!validResultFunctionUniqueName.isPresent(),
                "unique name of the result validity function has been already set");

        this.validResultFunctionUniqueName = Optional.of(uniqueName);
    }
}
