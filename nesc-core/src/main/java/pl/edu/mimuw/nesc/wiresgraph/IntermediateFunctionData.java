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
     * Value indicating if the command or event does not return any value.
     */
    private final boolean returnsVoid;

    /**
     * AST node for the creation of implementation of the intermediate function.
     * It contains proper prototype but empty body.
     */
    private final FunctionDecl intermediateFunction;

    /**
     * Saves the values from parameters in member fields.
     *
     * @param uniqueName Unique name of the intermediate function.
     * @param argumentsNames Names of arguments for the intermediate function.
     * @param returnsVoid Value indicating if the function returns void.
     * @param intermediateFunTemplate AST node for the creation of intermediate
     *                                function.
     * @throws NullPointerException One of the reference arguments is
     *                              <code>null</code>.
     * @throws IllegalArgumentException Unique name is an empty string.
     */
    IntermediateFunctionData(String uniqueName, ImmutableList<String> argumentsNames,
            boolean returnsVoid, FunctionDecl intermediateFunTemplate) {
        super(uniqueName);  // throws if 'uniqueName' is null or empty

        checkNotNull(argumentsNames, "names of parameters cannot be null");
        checkNotNull(intermediateFunTemplate, "the template of intermediate function cannot be null");

        this.parametersNames = argumentsNames;
        this.returnsVoid = returnsVoid;
        this.intermediateFunction = intermediateFunTemplate;
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
}
