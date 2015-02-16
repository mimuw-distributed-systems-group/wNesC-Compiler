package pl.edu.mimuw.nesc.finalreduce;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.AddressOf;
import pl.edu.mimuw.nesc.ast.gen.AstType;
import pl.edu.mimuw.nesc.ast.gen.Cast;
import pl.edu.mimuw.nesc.ast.gen.CompoundStmt;
import pl.edu.mimuw.nesc.ast.gen.ExprTransformer;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;

/**
 * <p>Class responsible for transforming expressions whose values are of
 * external base types.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ExternalExprTransformer extends ExprTransformer<ExternalExprBlockData> {
    public ExternalExprTransformer(NameMangler nameMangler) {
        super(new ExternalExprTransformation(nameMangler));
    }

    @Override
    public ExternalExprBlockData visitAddressOf(AddressOf expr, ExternalExprBlockData arg) {
        expr.getArgument().setIsNxTransformed(true);
        super.visitAddressOf(expr, arg);
        return arg;
    }

    @Override
    public ExternalExprBlockData visitCast(Cast expr, ExternalExprBlockData arg) {
        /* If the target cast type is an external base type, replace it with
           corresponding non-external type. */
        if (expr.getAsttype().getType().get().isExternalBaseType()) {
            final AstType newAstType = expr.getAsttype().getType().get().toAstType();
            if (newAstType.getDeclarator().isPresent()) {
                throw new RuntimeException("external base type specified with a declarator");
            }
            expr.setAsttype(newAstType);
        }

        super.visitCast(expr, arg);
        return arg;
    }

    @Override
    public ExternalExprBlockData visitCompoundStmt(CompoundStmt stmt, ExternalExprBlockData arg) {
        final Optional<String> atomicVarUniqueName = stmt.getAtomicVariableUniqueName();
        return atomicVarUniqueName == null || !atomicVarUniqueName.isPresent()
                ? arg.modifyEnclosingBlock(stmt)
                : arg;
    }
}
