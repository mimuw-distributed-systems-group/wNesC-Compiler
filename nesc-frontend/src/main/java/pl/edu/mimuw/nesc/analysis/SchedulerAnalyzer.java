package pl.edu.mimuw.nesc.analysis;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.Component;
import pl.edu.mimuw.nesc.ast.gen.Configuration;
import pl.edu.mimuw.nesc.ast.gen.Interface;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.type.FunctionType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.type.VoidType;
import pl.edu.mimuw.nesc.declaration.nesc.ModuleDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ComponentRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
import pl.edu.mimuw.nesc.common.SchedulerSpecification;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidSchedulerError;
import pl.edu.mimuw.nesc.problem.issue.InvalidTaskInterfaceError;
import pl.edu.mimuw.nesc.problem.issue.Issue;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that is responsible for the analysis of scheduler for NesC
 * tasks.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SchedulerAnalyzer {
    /**
     * Number of parameters of the parameterised interface a scheduler shall
     * provide to connect with tasks.
     */
    public static final int TASK_INTERFACE_PARAMS_COUNT = 1;

    /**
     * Type that the event for running tasks shall have.
     */
    public static final Type RUN_TASK_EVENT_TYPE = new FunctionType(new VoidType(), new Type[0], false);

    /**
     * Specification of the scheduler.
     */
    private final SchedulerSpecification specification;

    /**
     * Object that will be notified about all detected errors.
     */
    private final List<Issue> issues = new ArrayList<>();

    /**
     * Unmodifiable view of the issues list.
     */
    private final List<Issue> unmodifiableIssues = Collections.unmodifiableList(issues);

    /**
     * Initialize this analyzer to check if a scheduler matches the given
     * specification.
     *
     * @param specification Specification of the scheduler.
     * @throws NullPointerException One of the arguments is <code>null</code>.
     */
    public SchedulerAnalyzer(SchedulerSpecification specification) {
        checkNotNull(specification, "the scheduler specification cannot be null");
        this.specification = specification;
    }

    /**
     * Get an unmodifiable view of the list with issues found by this analyzer.
     *
     * @return Unmodifiable view of the list with issues.
     */
    public List<Issue> getIssues() {
        return unmodifiableIssues;
    }

    /**
     * Performs analysis of the scheduler that is the given node and reports all
     * detected errors to the error helper given at construction.
     *
     * @param schedulerNode Node that is the scheduler.
     */
    public void analyzeScheduler(Node schedulerNode) {
        if (!(schedulerNode instanceof Component)) {
            issues.add(InvalidSchedulerError.componentExpected(specification.getComponentName()));
            return;
        }

        final Component scheduler = (Component) schedulerNode;
        final SymbolTable<ObjectDeclaration> specObjects = scheduler.getSpecificationEnvironment().getObjects();

        // Check if the scheduler provides a proper parameterised interface

        boolean interfaceFound = false;

        for (Map.Entry<String, ObjectDeclaration> specEntry : specObjects.getAll()) {
            final ObjectDeclaration declaration = specEntry.getValue();

            if (analyzeTaskInterfaceInScheduler(declaration)) {
                interfaceFound = true;
                break;
            }
        }

        if (!interfaceFound) {
            issues.add(InvalidSchedulerError.absentTaskInterfaceInScheduler(specification.getComponentName(),
                    specification.getInterfaceNameInScheduler()));
        }

        /* Check if the scheduler instantiates a module with tasks (if it is
           configuration). */
        analyzeSchedulerComponents((Component) schedulerNode);
    }

    private boolean analyzeTaskInterfaceInScheduler(ObjectDeclaration declaration) {
        if (!declaration.getName().equals(specification.getInterfaceNameInScheduler())
                || declaration.getKind() != ObjectKind.INTERFACE) {
            return false;
        }

        final InterfaceRefDeclaration ifaceRefDeclaration = (InterfaceRefDeclaration) declaration;
        final Optional<ImmutableList<Optional<Type>>> instanceParams =
                ifaceRefDeclaration.getInstanceParameters();
        final Optional<? extends ErroneousIssue> error;

        if (!ifaceRefDeclaration.isProvides()) {
            error = Optional.of(InvalidSchedulerError.providedInterfaceExpected(specification.getComponentName(),
                        specification.getInterfaceNameInScheduler()));
        } else if (!ifaceRefDeclaration.getIfaceName().equals(specification.getTaskInterfaceName())) {
            error = Optional.of(InvalidSchedulerError.invalidInterfaceProvided(specification.getComponentName(),
                        specification.getInterfaceNameInScheduler(), ifaceRefDeclaration.getIfaceName(),
                        specification.getTaskInterfaceName()));
        } else if (!instanceParams.isPresent()) {
            error = Optional.of(InvalidSchedulerError.parameterisedInterfaceExpected(specification.getComponentName(),
                        specification.getInterfaceNameInScheduler()));
        } else if (instanceParams.get().size() != TASK_INTERFACE_PARAMS_COUNT) {
            error = Optional.of(InvalidSchedulerError.invalidInterfaceParamsCount(specification.getComponentName(),
                    specification.getInterfaceNameInScheduler(), instanceParams.get().size(), TASK_INTERFACE_PARAMS_COUNT));
        } else {
            error = Optional.absent();
        }

        if (error.isPresent()) {
            issues.add(error.get());
        }

        return true;
    }

    private void analyzeSchedulerComponents(Component schedulerComponent) {
        if (!(schedulerComponent instanceof Configuration)) {
            return;
        }

        final Environment implEnvironment = schedulerComponent.getImplementation().getEnvironment();

        for (Map.Entry<String, ObjectDeclaration> envEntry : implEnvironment.getObjects().getAll()) {
            if (envEntry.getValue().getKind() != ObjectKind.COMPONENT) {
                continue;
            }

            final ComponentRefDeclaration declaration = (ComponentRefDeclaration) envEntry.getValue();

            if (!(declaration.getComponentDeclaration().get() instanceof ModuleDeclaration)) {
                continue;
            }

            final ModuleDeclaration moduleDeclaration = (ModuleDeclaration) declaration.getComponentDeclaration().get();

            if (declaration.getAstComponentRef().getIsAbstract()
                    && !moduleDeclaration.getModuleTable().getTasks().isEmpty()) {
                issues.add(InvalidSchedulerError.configurationWithGenericModuleWithTask(specification.getComponentName(),
                        moduleDeclaration.getName()));
                return;
            }
        }
    }

    public void analyzeTaskInterface(Interface iface) {

        // Check if the interface is not generic

        if (iface.getParameters().isPresent()) {
            issues.add(InvalidTaskInterfaceError.expectedNonGenericInterface(
                    specification.getTaskInterfaceName()));
        }

        /* Check if the event for running task and command for posting it are
           correctly declared. */

        final SymbolTable<ObjectDeclaration> ifaceObjects = iface.getDeclarationEnvironment().getObjects();
        boolean postTaskCmdPresent = false, runTaskEventPresent = false;

        for (Map.Entry<String, ObjectDeclaration> ifaceEntry : ifaceObjects.getAll()) {
            final ObjectDeclaration declaration = ifaceEntry.getValue();
            postTaskCmdPresent = postTaskCmdPresent || analyzePostTaskCommand(declaration);
            runTaskEventPresent = runTaskEventPresent || analyzeRunTaskEvent(declaration);
        }

        if (!postTaskCmdPresent) {
            issues.add(InvalidTaskInterfaceError.postTaskCommandAbsent(specification.getTaskInterfaceName(),
                    specification.getTaskPostCommandName()));
        }

        if (!runTaskEventPresent) {
            issues.add(InvalidTaskInterfaceError.runTaskEventAbsent(specification.getTaskInterfaceName(),
                    specification.getTaskRunEventName()));
        }
    }

    private boolean analyzePostTaskCommand(ObjectDeclaration declaration) {
        if (!declaration.getName().equals(specification.getTaskPostCommandName())
                || declaration.getKind() != ObjectKind.FUNCTION) {
            return false;
        }

        final FunctionDeclaration funDeclaration = (FunctionDeclaration) declaration;
        final Optional<? extends ErroneousIssue> error;

        if (funDeclaration.getFunctionType() != FunctionDeclaration.FunctionType.COMMAND) {
            error = Optional.of(InvalidTaskInterfaceError.postTaskIsNotCommand(specification.getTaskInterfaceName(),
                        specification.getTaskPostCommandName()));
        } else if (!funDeclaration.getAstFunctionDeclarator().getParameters().isEmpty()) {
            error = Optional.of(InvalidTaskInterfaceError.postTaskCommandParametersPresent(
                    specification.getTaskInterfaceName(), specification.getTaskPostCommandName(),
                    funDeclaration.getAstFunctionDeclarator().getParameters().size()));
        } else if (funDeclaration.getType().isPresent()) {
            final FunctionType funType = (FunctionType) funDeclaration.getType().get();
            if (!funType.getReturnType().isIntegerType()) {
                error = Optional.of(InvalidTaskInterfaceError.invalidPostTaskCommandReturnType(
                        specification.getTaskInterfaceName(), specification.getTaskPostCommandName(),
                        funType.getReturnType()));
            } else {
                error = Optional.absent();
            }
        } else {
            error = Optional.absent();
        }

        if (error.isPresent()) {
            issues.add(error.get());
        }

        return true;
    }

    private boolean analyzeRunTaskEvent(ObjectDeclaration declaration) {
        if (!declaration.getName().equals(specification.getTaskRunEventName())
                || declaration.getKind() != ObjectKind.FUNCTION) {
            return false;
        }

        final FunctionDeclaration funDeclaration = (FunctionDeclaration) declaration;
        final Optional<? extends ErroneousIssue> error;

        if (funDeclaration.getFunctionType() != FunctionDeclaration.FunctionType.EVENT) {
            error = Optional.of(InvalidTaskInterfaceError.runTaskIsNotEvent(specification.getTaskInterfaceName(),
                        specification.getTaskRunEventName()));
        } else if (funDeclaration.getType().isPresent()
                && !funDeclaration.getType().get().isCompatibleWith(RUN_TASK_EVENT_TYPE)) {
            error = Optional.of(InvalidTaskInterfaceError.invalidRunTaskEventType(specification.getTaskInterfaceName(),
                        specification.getTaskRunEventName(), RUN_TASK_EVENT_TYPE, funDeclaration.getType().get()));
        } else {
            error = Optional.absent();
        }

        if (error.isPresent()) {
            issues.add(error.get());
        }

        return true;
    }
}
