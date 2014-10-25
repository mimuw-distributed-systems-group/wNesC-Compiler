package pl.edu.mimuw.nesc.analysis;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.type.UnknownType;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidInterfaceInstantiationError;
import pl.edu.mimuw.nesc.problem.issue.InvalidInterfaceParameterError;

import java.util.Iterator;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkState;
import static pl.edu.mimuw.nesc.problem.issue.InvalidInterfaceInstantiationError.InterfaceRefProblemKind;
import static pl.edu.mimuw.nesc.problem.issue.InvalidInterfaceParameterError.IfaceParamProblemKind;

/**
 * A class that is responsible for analysis of constructs that are tightly
 * connected with the NesC language, e.g. <code>provides</code> or
 * <code>uses</code> specification elements.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NescAnalysis {
    /**
     * Check the correctness of an interface instantiation. The given error
     * helper is notified about detected errors.
     *
     * @param ifaceRefDecl Declaration object for interface reference that is
     *                     checked.
     * @param errorHelper Object that will be notified about detected errors.
     */
    public static void checkInterfaceInstantiation(InterfaceRefDeclaration ifaceRefDecl,
            ErrorHelper errorHelper) {

        if (!ifaceRefDecl.getIfaceDeclaration().isPresent()) {
            return;
        }

        final Optional<LinkedList<Declaration>> paramsDecls = ifaceRefDecl.getIfaceDeclaration()
                .get().getAstInterface().getParameters();
        final Optional<LinkedList<Expression>> instantiationTypes = ifaceRefDecl
                .getAstInterfaceRef().getArguments();

        final Optional<InterfaceRefProblemKind> problemKind;
        int definitionParamsCount = 0, providedParamsCount = 0;

        if (paramsDecls.isPresent() && !instantiationTypes.isPresent()) {
            problemKind = Optional.of(InterfaceRefProblemKind.MISSING_PARAMETERS);
        } else if (!paramsDecls.isPresent() && instantiationTypes.isPresent()) {
            problemKind = Optional.of(InterfaceRefProblemKind.UNEXPECTED_PARAMETERS);
        } else if (paramsDecls.isPresent()) {

            // Interface is generic and parameters are provided

            final LinkedList<Declaration> decls = paramsDecls.get();
            final LinkedList<Expression> types = instantiationTypes.get();

            definitionParamsCount = decls.size();
            providedParamsCount = types.size();

            if (definitionParamsCount != providedParamsCount) {
                problemKind = Optional.of(InterfaceRefProblemKind.PARAMETERS_COUNTS_DIFFER);
            } else {
                checkInterfaceParameters(decls.iterator(), types.iterator(),
                        ifaceRefDecl.getIfaceName(), errorHelper);
                problemKind = Optional.absent();
            }
        } else {
            // Interface is not generic and parameters are not provided
            problemKind = Optional.absent();
        }

        if (problemKind.isPresent()) {
            final InvalidInterfaceInstantiationError error = new InvalidInterfaceInstantiationError(
                    problemKind.get(),
                    ifaceRefDecl.getIfaceName(),
                    definitionParamsCount,
                    providedParamsCount
            );

            errorHelper.error(ifaceRefDecl.getAstInterfaceRef().getLocation(),
                    ifaceRefDecl.getAstInterfaceRef().getEndLocation(), error);
        }
    }

    /**
     * Check the constraints for generic parameters in an interface reference
     * and report detected errors.
     */
    private static void checkInterfaceParameters(Iterator<Declaration> declIt,
            Iterator<Expression> typeIt, String interfaceName, ErrorHelper errorHelper) {

        int paramNum = 0;

        while (declIt.hasNext()) {

            ++paramNum;
            final Declaration decl = declIt.next();
            final Expression expr = typeIt.next();

            // Currently the condition is never true
            if (decl instanceof ErrorDecl || expr instanceof ErrorExpr) {
                continue;
            }

            checkState(decl instanceof TypeParmDecl, "unexpected class of a generic parameter declaration: "
                    + decl.getClass().getCanonicalName());
            checkState(expr instanceof TypeArgument, "unexpected class of a type for generic parameter: "
                    + expr.getClass().getCanonicalName());

            final TypeParmDecl typeParmDecl = (TypeParmDecl) decl;
            final TypeArgument typeArgument = (TypeArgument) expr;

            if (!typeParmDecl.getDeclaration().getDenotedType().isPresent()
                    || !typeArgument.getAsttype().getType().isPresent()) {
                continue;
            }

            checkInterfaceParameter(
                    (UnknownType) typeParmDecl.getDeclaration().getDenotedType().get(),
                    typeArgument.getAsttype().getType().get(),
                    interfaceName,
                    typeArgument,
                    paramNum,
                    errorHelper
            );
        }
    }

    private static void checkInterfaceParameter(UnknownType definitionType, Type providedType,
            String interfaceName, TypeArgument expr, int paramNum, ErrorHelper errorHelper) {

        final Optional<IfaceParamProblemKind> problem;

        if (providedType.isFunctionType()) {
            problem = Optional.of(IfaceParamProblemKind.FUNCTION_TYPE);
        } else if (providedType.isArrayType()) {
            problem = Optional.of(IfaceParamProblemKind.ARRAY_TYPE);
        } else if (!providedType.isComplete()) {
            problem = Optional.of(IfaceParamProblemKind.INCOMPLETE_TYPE);
        } else if (definitionType.isUnknownIntegerType() && !providedType.isIntegerType()) {
            problem = Optional.of(IfaceParamProblemKind.INTEGER_TYPE_EXPECTED);
        } else if (definitionType.isUnknownArithmeticType() && !providedType.isArithmetic()) {
            problem = Optional.of(IfaceParamProblemKind.ARITHMETIC_TYPE_EXPECTED);
        } else {
            problem = Optional.absent();
        }

        if (problem.isPresent()) {
            final ErroneousIssue error = new InvalidInterfaceParameterError(problem.get(),
                    interfaceName, definitionType, providedType, paramNum);
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
        }
    }

    /**
     * Private constructor to prevent from instantiating this class.
     */
    private NescAnalysis() {
    }
}
