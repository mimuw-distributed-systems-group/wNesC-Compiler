package pl.edu.mimuw.nesc.semantic.nesc;

import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.issue.NescIssue;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescEntityBuilder extends ExceptionVisitor<Void, Void> {

    private final ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder;
    private final NescEntityEnvironment nescEnvironment;

    public NescEntityBuilder(ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                             NescEntityEnvironment nescEnvironment) {
        this.issuesMultimapBuilder = issuesMultimapBuilder;
        this.nescEnvironment = nescEnvironment;
    }

    public Void visitInterface(Interface iface, Void arg) {
        final InterfaceBuilder builder = new InterfaceBuilder(nescEnvironment, issuesMultimapBuilder, iface);
        builder.build();
        return null;
    }
    public Void visitConfiguration(Configuration elem, Void arg) {
        return null;
    }
    public Void visitModule(Module module, Void arg) {
        final ModuleBuilder builder = new ModuleBuilder(nescEnvironment, issuesMultimapBuilder, module);
        builder.build();
        return null;
    }
    public Void visitBinaryComponent(BinaryComponent elem, Void arg) {
        return null;
    }
}
