package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.base.Optional;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pl.edu.mimuw.nesc.abi.Endianness;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.GccAttribute;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.external.ExternalScheme;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidExternalBaseAttributeError;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Object responsible for analysis of GCC attributes 'nx_base_be' and
 * 'nx_base_le' that are applied to type definitions that define external base
 * types.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class ExternalBaseAttributeAnalyzer implements AttributeSmallAnalyzer {
    /**
     * Names of the attributes that specify external base types.
     */
    private static final String NAME_NX_BASE_BE = "nx_base_be";
    private static final String NAME_NX_BASE_LE = "nx_base_le";

    /**
     * Regular expression that tests parameter of the attribute.
     */
    private static final Pattern PATTERN_PARAMETER_VALUE =
            Pattern.compile("(?<littleEndian>le)?(?<unsigned>u)?int(?<bitsCount>8|16|32|64)");

    /**
     * Object that will be notified about detected errors.
     */
    private final ErrorHelper errorHelper;

    ExternalBaseAttributeAnalyzer(ErrorHelper errorHelper) {
        checkNotNull(errorHelper, "error helper cannot be null");
        this.errorHelper = errorHelper;
    }

    @Override
    public void analyzeAttribute(List<Attribute> attributes, Declaration declaration, Environment environment) {
        int invalidAttributesCount = 0, allAttributesCount = 0;
        Optional<ExternalScheme> externalScheme = Optional.absent();

        for (Attribute attribute : attributes) {
            if (!(attribute instanceof GccAttribute)) {
                continue;
            }

            final GccAttribute gccAttribute = (GccAttribute) attribute;

            if (isExternalBaseAttribute(gccAttribute.getName().getName())) {
                externalScheme = analyzeAttribute(gccAttribute.getArguments(),
                        gccAttribute.getName().getName(), gccAttribute.getLocation(),
                        gccAttribute.getEndLocation());

                if (!externalScheme.isPresent()) {
                    ++invalidAttributesCount;
                }
                ++allAttributesCount;
            }
        }

        if (allAttributesCount > 1 && invalidAttributesCount == 0) {
            errorHelper.error(attributes.get(0).getLocation(), attributes.get(attributes.size() - 1).getEndLocation(),
                    InvalidExternalBaseAttributeError.tooManyAttributes());
        } else if (allAttributesCount == 1 && invalidAttributesCount == 0) {
            if (!(declaration instanceof TypenameDeclaration)) {
                errorHelper.error(attributes.get(0).getLocation(), attributes.get(attributes.size() - 1).getEndLocation(),
                        InvalidExternalBaseAttributeError.appliedNotToTypedef());
            } else {
                final TypenameDeclaration typenameDeclaration = (TypenameDeclaration) declaration;
                if (typenameDeclaration.getDenotedType().isPresent()
                        && !typenameDeclaration.getDenotedType().get().isIntegerType()) {
                    errorHelper.error(attributes.get(0).getLocation(), attributes.get(attributes.size() - 1).getEndLocation(),
                            InvalidExternalBaseAttributeError.appliedNotToIntegerType());
                } else {
                    typenameDeclaration.addExternalScheme(externalScheme.get());
                }
            }
        }
    }

    private boolean isExternalBaseAttribute(String name) {
        return name.equals(NAME_NX_BASE_BE) || name.equals(NAME_NX_BASE_LE);
    }

    private Optional<ExternalScheme> analyzeAttribute(Optional<LinkedList<Expression>> parameters,
                String attributeName, Location startLoc, Location endLoc) {
        final ErroneousIssue error;

        if (!parameters.isPresent()) {
            error = InvalidExternalBaseAttributeError.parametersMissing(attributeName);
        } else if (parameters.get().size() != 1) {
            error = InvalidExternalBaseAttributeError.invalidParamsCount(attributeName,
                    parameters.get().size());
        } else if (!(parameters.get().getFirst() instanceof Identifier)) {
            error = InvalidExternalBaseAttributeError.identifierExpected(attributeName,
                    parameters.get().getFirst());
        } else {
            final Identifier paramValue = (Identifier) parameters.get().getFirst();
            final Matcher valueMatcher = PATTERN_PARAMETER_VALUE.matcher(paramValue.getName());

            if (!valueMatcher.matches() || (valueMatcher.group("littleEndian") != null) != attributeName.endsWith("le")) {
                error = InvalidExternalBaseAttributeError.invalidParameterValue(
                        attributeName, paramValue.getName());
            } else {
                final Endianness endianness = valueMatcher.group("littleEndian") != null
                        ? Endianness.LITTLE_ENDIAN
                        : Endianness.BIG_ENDIAN;
                final ExternalScheme result = new ExternalScheme(
                        endianness,
                        Integer.parseInt(valueMatcher.group("bitsCount")) / 8,
                        valueMatcher.group("unsigned") != null
                );
                return Optional.of(result);
            }
        }

        errorHelper.error(startLoc, endLoc, error);
        return Optional.absent();
    }
}
