package pl.edu.mimuw.nesc.intermediate;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.nesc.ast.NescCallKind;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.FunctionCall;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.GenericCall;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.IdentifierDeclarator;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;
import pl.edu.mimuw.nesc.ast.gen.InterfaceDeref;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Module;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Rid;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.util.AstUtils;
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
 *     <li>adding definitions of implemented commands and events to the multimap
 *     of specification elements</li>
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
     * Map with functions that are implementation of specification elements.
     */
    private final Multimap<String, FunctionDecl> specificationElements;

    /**
     * Initializes this transformer to use the given graph and extend the given
     * map of specification elements.
     *
     * @param graph Graph with names of intermediate functions.
     * @param specificationElements Map to be extended with information about
     *                              encountered implementations of commands and
     *                              events.
     */
    public IntermediateTransformer(WiresGraph graph, Multimap<String, FunctionDecl> specificationElements) {
        checkNotNull(graph, "the wires graph cannot be null");
        checkNotNull(specificationElements, "specification elements cannot be null");

        this.wiresGraph = graph;
        this.specificationElements = specificationElements;
    }

    @Override
    public Optional<String> visitModule(Module module, Optional<String> componentName) {
        return Optional.of(module.getName().getName());
    }

    @Override
    public Optional<String> visitFunctionDecl(FunctionDecl functionDecl, Optional<String> componentName) {
        if (removeKeywords(functionDecl.getModifiers())) {
            NestedDeclarator first = (NestedDeclarator) functionDecl.getDeclarator();
            Declarator second = first.getDeclarator().get();

            while (!(second instanceof InterfaceRefDeclarator)
                    && !(second instanceof IdentifierDeclarator)) {
                first = (NestedDeclarator) second;
                second = first.getDeclarator().get();
            }

            // Add the definition to the specification elements map

            final String specificationElementName = second instanceof InterfaceRefDeclarator
                    ? ((InterfaceRefDeclarator) second).getName().getName()
                    : ((IdentifierDeclarator) second).getName();
            final String fullName = format("%s.%s", componentName.get(), specificationElementName);
            specificationElements.put(fullName, functionDecl);

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
        removeKeywords(dataDecl.getModifiers());
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
