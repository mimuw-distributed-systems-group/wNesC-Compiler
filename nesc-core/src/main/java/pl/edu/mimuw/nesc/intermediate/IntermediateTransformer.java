package pl.edu.mimuw.nesc.intermediate;

import com.google.common.base.Optional;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.nesc.ast.NescCallKind;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.wiresgraph.SpecificationElementNode;
import pl.edu.mimuw.nesc.wiresgraph.WiresGraph;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * <p>A class responsible for making the following changes to visited nodes:
 * </p>
 *
 * <ol>
 *     <li>replacing <code>call</code> and <code>signal</code> expressions with
 *     calls to intermediate functions</li>
 *     <li>removing keywords: <code>async</code>, <code>norace</code>,
 *     <code>command</code>, <code>event</code>, <code>default</code></li>
 *     <li>removing <code>InterfaceRefDeclarator</code> AST nodes</li>
 *     <li>moving instance parameters of functions definitions to the beginning
 *     of the list of normal parameters</li>
 *     <li>removing NesC attributes</li>
 * </ol>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IntermediateTransformer extends IdentityVisitor<Optional<String>> {
    /**
     * Graph with names of intermediate functions.
     */
    private final WiresGraph wiresGraph;

    /**
     * Initializes this transformer to use the given graph and extend the given
     * map of specification elements.
     *
     * @param graph Graph with names of intermediate functions.
     */
    public IntermediateTransformer(WiresGraph graph) {
        checkNotNull(graph, "the wires graph cannot be null");

        this.wiresGraph = graph;
    }

    @Override
    public Optional<String> visitModule(Module module, Optional<String> componentName) {
        return Optional.of(module.getName().getName());
    }

    @Override
    public Optional<String> visitFunctionDecl(FunctionDecl functionDecl, Optional<String> componentName) {
        // Modify command or event
        if (removeKeywords(functionDecl.getModifiers())) {
            NestedDeclarator first = (NestedDeclarator) functionDecl.getDeclarator();
            Declarator second = first.getDeclarator().get();

            while (!(second instanceof InterfaceRefDeclarator)
                    && !(second instanceof IdentifierDeclarator)) {
                first = (NestedDeclarator) second;
                second = first.getDeclarator().get();
            }

            // Remove the potential interface reference declarator

            if (second instanceof InterfaceRefDeclarator) {
                first.setDeclarator(((NestedDeclarator) second).getDeclarator());
            }

            /* Move generic parameters declarations to list of normal parameters
               declarations. */

            final FunctionDeclarator funDeclarator = (FunctionDeclarator) first;
            if (funDeclarator.getGenericParameters().isPresent()) {
                funDeclarator.getParameters().addAll(0, funDeclarator.getGenericParameters().get());
                funDeclarator.setGenericParameters(Optional.<LinkedList<Declaration>>absent());
            }
        }

        // Remove NesC attributes
        TypeElementUtils.removeNescTypeElements(functionDecl.getAttributes());

        return componentName;
    }

    @Override
    public Optional<String> visitFunctionCall(FunctionCall call, Optional<String> componentName) {
        switch (call.getCallKind()) {
            case COMMAND_CALL:
            case EVENT_SIGNAL:
                call.setCallKind(NescCallKind.NORMAL_CALL);
                final String nodeName = format("%s.%s", componentName.get(), getNodeNameSuffix(call.getFunction()));
                final Optional<GenericCall> genericCall = call.getFunction() instanceof GenericCall
                        ? Optional.of((GenericCall) call.getFunction())
                        : Optional.<GenericCall>absent();

                // Add values of generic parameters to list of normal parameters
                if (genericCall.isPresent()) {
                    call.getArguments().addAll(0, genericCall.get().getArguments());
                }

                // Set the name of the appropriate function
                final SpecificationElementNode node = wiresGraph.requireNode(nodeName);
                call.setFunction(AstUtils.newIdentifier(node.getEntityData().getUniqueName()));

                break;
            case POST_TASK:
                throw new IllegalArgumentException("a node with task post expression encountered");
        }

        return componentName;
    }

    private String getNodeNameSuffix(Expression cmdOrEventRef) {
        if (cmdOrEventRef instanceof GenericCall) {
            cmdOrEventRef = ((GenericCall) cmdOrEventRef).getName();
        }

        if (cmdOrEventRef instanceof InterfaceDeref) {
            final InterfaceDeref interfaceDeref = (InterfaceDeref) cmdOrEventRef;
            final Identifier interfaceRefName = (Identifier) interfaceDeref.getArgument();
            return format("%s.%s", interfaceRefName.getName(), interfaceDeref.getMethodName().getName());
        } else {
            return ((Identifier) cmdOrEventRef).getName();
        }
    }

    @Override
    public Optional<String> visitDataDecl(DataDecl dataDecl, Optional<String> componentName) {
        TypeElementUtils.removeNescTypeElements(dataDecl.getModifiers());
        return componentName;
    }

    @Override
    public Optional<String> visitVariableDecl(VariableDecl variableDecl, Optional<String> componentName) {
        TypeElementUtils.removeNescTypeElements(variableDecl.getAttributes());
        return componentName;
    }

    @Override
    public Optional<String> visitQualifiedDeclarator(QualifiedDeclarator declarator, Optional<String> componentName) {
        TypeElementUtils.removeNescTypeElements(declarator.getModifiers());
        return componentName;
    }

    @Override
    public Optional<String> visitTagRef(TagRef tagReference, Optional<String> componentName) {
        TypeElementUtils.removeNescTypeElements(tagReference.getAttributes());
        return componentName;
    }

    private boolean removeKeywords(List<TypeElement> typeElements) {
        final Iterator<TypeElement> typeElementIt = typeElements.iterator();
        boolean cmdOrEventOccurred = false;

        while (typeElementIt.hasNext()) {
            final TypeElement typeElement = typeElementIt.next();

            if (!(typeElement instanceof Rid)) {
                continue;
            }

            final Rid rid = (Rid) typeElement;
            switch (rid.getId()) {
                case COMMAND:
                case EVENT:
                    cmdOrEventOccurred = true;
                case NORACE:
                case ASYNC:
                case DEFAULT:
                case TASK:
                    typeElementIt.remove();
            }
        }

        return cmdOrEventOccurred;
    }
}
