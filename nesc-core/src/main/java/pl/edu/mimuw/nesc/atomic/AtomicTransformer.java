package pl.edu.mimuw.nesc.atomic;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.AtomicStmt;
import pl.edu.mimuw.nesc.ast.gen.CompoundStmt;
import pl.edu.mimuw.nesc.ast.gen.DoWhileStmt;
import pl.edu.mimuw.nesc.ast.gen.ForStmt;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.StmtTransformer;
import pl.edu.mimuw.nesc.ast.gen.SwitchStmt;
import pl.edu.mimuw.nesc.ast.gen.WhileStmt;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;

import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Transformer that is responsible for replacing atomic statements to
 * constructs that guarantee their atomic execution.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class AtomicTransformer extends StmtTransformer<AtomicBlockData> {
    public AtomicTransformer(AtomicSpecification atomicSpecification, NameMangler nameMangler) {
        super(new AtomicTransformation(atomicSpecification, nameMangler));
    }

    @Override
    public AtomicBlockData visitAtomicStmt(AtomicStmt stmt, AtomicBlockData arg) {
        throw new IllegalStateException("entered atomic statement - it implies it hasn't been processed earlier");
    }

    @Override
    public AtomicBlockData visitFunctionDecl(FunctionDecl node, AtomicBlockData arg) {
        return AtomicBlockData.builder(arg)
                .functionReturnType(AstUtils.extractReturnType(node))
                .insideAtomicFunction(VariousUtils.getBooleanValue(node.getIsAtomic()))
                .build();
    }

    @Override
    public AtomicBlockData visitCompoundStmt(CompoundStmt stmt, AtomicBlockData arg) {
        if (stmt.getAtomicVariableUniqueName() == null) {
            stmt.setAtomicVariableUniqueName(Optional.<String>absent());
        }

        checkState(!stmt.getAtomicVariableUniqueName().isPresent() || !arg.getAtomicVariableUniqueName().isPresent(),
                "block executed atomically inside such block encountered");

        final AtomicBlockData result = AtomicBlockData.builder(arg)
                .atomicVariableUniqueName(arg.getAtomicVariableUniqueName().or(stmt.getAtomicVariableUniqueName()).orNull())
                .insideBreakableAtomic(!arg.isInsideAtomicBlock() && arg.isInsideLoopOrSwitch()
                        && stmt.getAtomicVariableUniqueName().isPresent()
                        || arg.isInsideBreakableAtomic())
                .build();
        super.visitCompoundStmt(stmt, result);

        return result;
    }

    @Override
    public AtomicBlockData visitWhileStmt(WhileStmt node, AtomicBlockData arg) {
        final AtomicBlockData newData = blockDataForConditionalStmt(arg);
        super.visitWhileStmt(node, newData);
        return newData;
    }

    @Override
    public AtomicBlockData visitDoWhileStmt(DoWhileStmt node, AtomicBlockData arg) {
        final AtomicBlockData newData = blockDataForConditionalStmt(arg);
        super.visitDoWhileStmt(node, newData);
        return newData;
    }

    @Override
    public AtomicBlockData visitSwitchStmt(SwitchStmt node, AtomicBlockData arg) {
        final AtomicBlockData newData = blockDataForConditionalStmt(arg);
        super.visitSwitchStmt(node, newData);
        return newData;
    }

    @Override
    public AtomicBlockData visitForStmt(ForStmt node, AtomicBlockData arg) {
        final AtomicBlockData newData = blockDataForConditionalStmt(arg);
        super.visitForStmt(node, arg);
        return newData;
    }

    private AtomicBlockData blockDataForConditionalStmt(AtomicBlockData oldData) {
        return AtomicBlockData.builder(oldData)
                .insideLoopOrSwitch(true)
                .insideBreakableAtomic(false)
                .build();
    }
}
