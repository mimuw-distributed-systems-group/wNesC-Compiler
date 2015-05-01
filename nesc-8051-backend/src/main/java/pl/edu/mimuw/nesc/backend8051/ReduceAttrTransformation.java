package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.AttrTransformation;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.GccAttribute;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.Plus;
import pl.edu.mimuw.nesc.ast.gen.TargetAttribute;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.compilation.CompilationListener;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.problem.NescWarning;
import pl.edu.mimuw.nesc.problem.issue.Issue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Transformation that prepares attributes that are present in a NesC
 * application for SDCC.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class ReduceAttrTransformation implements AttrTransformation<Void> {
    /**
     * Listener that will be notified about detected issues.
     */
    private final CompilationListener listener;

    /**
     * Initialize the transformation to notify the given listener about detected
     * issues.
     *
     * @param listener Listener that will be notified about issues.
     */
    ReduceAttrTransformation(CompilationListener listener) {
        checkNotNull(listener, "compilation listener cannot be null");
        this.listener = listener;
    }

    @Override
    public LinkedList<Attribute> transform(GccAttribute attr, Node containingNode, Void arg) {
        switch (attr.getName().getName()) {
            case "packed":
                return transformPacked();
            case "sfr":
                return transformSfr(attr);
            case "sfrx":
                return transformSfrx(attr);
            case "banked":
                return transformBanked(attr, containingNode);
            default:
                listener.warning(makeWarning("ignoring GCC attribute '"
                        + attr.getName().getName() + "'", attr));
                return Lists.newList();
        }
    }

    @Override
    public LinkedList<Attribute> transform(TargetAttribute attr, Node containingNode, Void arg) {
        return Lists.<Attribute>newList(attr);
    }

    @Override
    public LinkedList<Attribute> transform(NescAttribute attr, Node containingNode, Void arg) {
        /* All NesC attributes are removed - however, there should not be any at
           this point. */
        return Lists.newList();
    }

    private LinkedList<Attribute> transformPacked() {
        /* 'packed' attribute can be safely removed as all structures and unions
            compiled with SDCC are packed. */
        return Lists.newList();
    }

    private LinkedList<Attribute> transformSfr(GccAttribute attr) {
        if (!attr.getArguments().isPresent() || attr.getArguments().get().size() < 2
                || !(attr.getArguments().get().getFirst() instanceof Identifier)) {
            listener.warning(makeWarningInvalidUsage(attr));
            return Lists.newList();
        }

        final Identifier fstParam = (Identifier) attr.getArguments().get().getFirst();
        final List<Expression> rest = attr.getArguments().get().subList(1, attr.getArguments().get().size());

        switch (fstParam.getName()) {
            case "sbit":
                return transformSfrSbit(attr, rest);
            case "sbyte":
                return transformSfrSbyte(attr, rest);
            default:
                listener.warning(makeWarningInvalidUsage(attr));
                return Lists.newList();
        }
    }

    private LinkedList<Attribute> transformSfrSbit(GccAttribute attr, List<Expression> params) {
        if (params.size() != 2) {
            listener.warning(makeWarningInvalidUsage(attr));
            return Lists.newList();
        }

        final LinkedList<Attribute> replacement = Lists.<Attribute>newList(
                AstUtils.newTargetAttribute0("__sbit"));
        replacement.add(AstUtils.newTargetAttribute1("__at", new Plus(Location.getDummyLocation(),
                params.get(0), params.get(1))));

        return replacement;
    }

    private LinkedList<Attribute> transformSfrSbyte(GccAttribute attr, List<Expression> params) {
        if (params.size() != 1) {
            listener.warning(makeWarningInvalidUsage(attr));
            return Lists.newList();
        }

        final LinkedList<Attribute> replacement = Lists.<Attribute>newList(
                AstUtils.newTargetAttribute0("__sfr"));
        replacement.add(AstUtils.newTargetAttribute1("__at", params.get(0)));

        return replacement;
    }

    private LinkedList<Attribute> transformSfrx(GccAttribute attr) {
        if (!attr.getArguments().isPresent() || attr.getArguments().get().size() != 2
                || !(attr.getArguments().get().getFirst() instanceof Identifier)) {
            listener.warning(makeWarningInvalidUsage(attr));
            return Lists.newList();
        }

        final Identifier firstParam = (Identifier) attr.getArguments().get().getFirst();
        if (!"sbyte".equals(firstParam.getName())) {
            listener.warning(makeWarningInvalidUsage(attr));
            return Lists.newList();
        }

        final LinkedList<Attribute> replacement = Lists.<Attribute>newList(
                AstUtils.newTargetAttribute0("__xdata"));
        replacement.add(AstUtils.newTargetAttribute1("__at", attr.getArguments().get().get(1)));

        return replacement;
    }

    private LinkedList<Attribute> transformBanked(GccAttribute gccAttr, Node containingNode) {
        if (gccAttr.getArguments().isPresent()) {
            listener.warning(makeWarningInvalidUsage(gccAttr));
            return Lists.newList();
        }

        if (containingNode instanceof FunctionDecl) {
            final FunctionDecl funDecl = (FunctionDecl) containingNode;
            DeclaratorUtils.setIsBanked(funDecl.getDeclarator(), true);
            return Lists.newList();
        } else if (containingNode instanceof DataDecl
                || containingNode instanceof VariableDecl) {
            final VariableDecl variableDecl;
            if (containingNode instanceof DataDecl) {
                final DataDecl dataDecl = (DataDecl) containingNode;

                if (dataDecl.getDeclarations().isEmpty()) {
                    listener.warning(makeWarningInvalidUsage(gccAttr));
                    return Lists.newList();
                } else if (!(dataDecl.getDeclarations().getFirst() instanceof VariableDecl)) {
                    listener.warning(makeWarningInvalidUsage(gccAttr));
                    return Lists.newList();
                } else if (dataDecl.getDeclarations().size() != 1) {
                    throw new IllegalStateException("unseparated declarations encountered");
                } else {
                    variableDecl = (VariableDecl) dataDecl.getDeclarations().getFirst();
                }
            } else {
                variableDecl = (VariableDecl) containingNode;
            }

            if (!variableDecl.getDeclarator().isPresent()) {
                listener.warning(makeWarningInvalidUsage(gccAttr));
                return Lists.newList();
            }

            final Optional<NestedDeclarator> deepestDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(variableDecl.getDeclarator().get());
            if (!deepestDeclarator.isPresent() || !(deepestDeclarator.get() instanceof FunctionDeclarator)) {
                listener.warning(makeWarningInvalidUsage(gccAttr));
                return Lists.newList();
            }
            ((FunctionDeclarator) deepestDeclarator.get()).setIsBanked(true);
            return Lists.newList();
        } else {
            listener.warning(makeWarningInvalidUsage(gccAttr));
            return Lists.newList();
        }
    }

    private NescWarning makeWarning(String msg, Node location) {
        return new NescWarning(location.getLocation(), Optional.of(location.getEndLocation()),
                Optional.<Issue.Code>absent(), msg);
    }

    private NescWarning makeWarningInvalidUsage(Attribute attr) {
        return makeWarning("ignoring incorrectly used attribute '"
                + attr.getName().getName() + "'", attr);
    }
}
