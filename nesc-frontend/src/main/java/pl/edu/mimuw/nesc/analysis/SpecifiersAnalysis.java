package pl.edu.mimuw.nesc.analysis;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pl.edu.mimuw.nesc.ast.Interval;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.LocationsPin;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.problem.ErrorHelper;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * A class with definitions that are intended to perform the analysis of
 * storage-class and function specifiers.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class SpecifiersAnalysis {
    /**
     * Various constants used in this class.
     */
    private static final String FMT_ERR_SPECIFIER_REPEATED = "'%s' specifier ignored because it has been already specified; remove it";
    private static final String FMT_ERR_GENERIC_PARAM_TYPEDEF_OTHER = "Cannot combine 'typedef' with other specifiers in a generic parameter declaration";
    private static final String FMT_ERR_GENERIC_PARAM_NORACE_OTHER = "Cannot use non-type specifiers other than 'norace' in a non-type generic parameter declaration";
    private static final String FMT_ERR_INSTANCE_PARAM_NORACE_OTHER = "Cannot use non-type specifiers other than 'norace' in an instance parameter declaration";


    /**
     * Mapping between values of non-type specifiers from <code>RID</code> type
     * to corresponding values from <code>NonTypeSpecifier</code> type.
     */
    private static final ImmutableMap<RID, NonTypeSpecifier> RID_MAP;
    static {
        final ImmutableMap.Builder<RID, NonTypeSpecifier> builder = ImmutableMap.builder();
        builder.put(RID.TYPEDEF, NonTypeSpecifier.TYPEDEF);
        builder.put(RID.EXTERN, NonTypeSpecifier.EXTERN);
        builder.put(RID.STATIC, NonTypeSpecifier.STATIC);
        builder.put(RID.REGISTER, NonTypeSpecifier.REGISTER);
        builder.put(RID.AUTO, NonTypeSpecifier.AUTO);
        builder.put(RID.COMMAND, NonTypeSpecifier.COMMAND);
        builder.put(RID.EVENT, NonTypeSpecifier.EVENT);
        builder.put(RID.TASK, NonTypeSpecifier.TASK);
        builder.put(RID.NORACE, NonTypeSpecifier.NORACE);
        builder.put(RID.ASYNC, NonTypeSpecifier.ASYNC);
        builder.put(RID.INLINE, NonTypeSpecifier.INLINE);
        builder.put(RID.DEFAULT, NonTypeSpecifier.DEFAULT);
        RID_MAP = builder.build();
    }

    /**
     * Assumes that given specifiers occurred in a generic parameter declaration
     * of a generic component and checks their validity. All detected errors are
     * emitted.
     *
     * @param specifiers Specifiers set with non-type specifiers from the
     *                   declaration.
     * @param allSpecifiersCount Count of all specifiers from the declaration of
     *                           the generic parameter (with repetitions).
     * @param specifiersInterval Interval that all specifiers are contained in.
     * @param errorHelper Object that will be notified about errors.
     * @throws NullPointerException <code>specifiers</code>,
     *                              <code>specifiersInterval</code> or
     *                              <code>errorHelper</code> is null.
     */
    public static void checkGenericParameterSpecifiers(SpecifiersSet specifiers,
            int allSpecifiersCount, Interval specifiersInterval, ErrorHelper errorHelper) {
        checkNotNull(specifiers, "specifiers cannot be null");
        checkNotNull(specifiersInterval, "the interval of specifiers cannot be null");
        checkNotNull(errorHelper, "the error helper cannot be null");

        final boolean isTypedef = specifiers.contains(NonTypeSpecifier.TYPEDEF);
        final boolean isNorace = specifiers.contains(NonTypeSpecifier.NORACE);
        final boolean typeSpecifiersPresent = allSpecifiersCount > specifiers.sizeWithRepetitions();

        if (isTypedef && (typeSpecifiersPresent || specifiers.size() > 1)) {
            errorHelper.error(
                    specifiersInterval.getLocation(),
                    specifiersInterval.getEndLocation(),
                    FMT_ERR_GENERIC_PARAM_TYPEDEF_OTHER
            );
        } else if (isTypedef && specifiers.sizeWithRepetitions() > 1) {
            specifiers.emitRepetitionWarnings();
        } else if (!isTypedef && isNorace && specifiers.size() > 1) {
            errorHelper.error(
                    specifiersInterval.getLocation(),
                    specifiersInterval.getEndLocation(),
                    FMT_ERR_GENERIC_PARAM_NORACE_OTHER
            );
        } else if (!isTypedef && isNorace && specifiers.sizeWithRepetitions() > 1) {
            specifiers.emitRepetitionWarnings();
        }
    }

    /**
     * Checks the specifiers in given instance parameters and reports all
     * detected errors to given error helper. If the parameters are absent, then
     * does nothing.
     *
     * @throws NullPointerException One of the arguments is null.
     */
    public static void checkInstanceParametersSpecifiers(Optional<LinkedList<Declaration>> instanceParameters,
            ErrorHelper errorHelper) {
        checkNotNull(instanceParameters, "instance parameters cannot be null");
        checkNotNull(errorHelper, "the error helper cannot be null");

        if (!instanceParameters.isPresent()) {
            return;
        }

        // Collect information about specifiers for all parameters
        final List<Declaration> params = instanceParameters.get();
        final InstanceParametersVisitor visitor = new InstanceParametersVisitor(errorHelper);
        for (Declaration declaration : params) {
            declaration.accept(visitor, null);
        }

        // Process the specifiers
        final ImmutableList<SpecifiersSet> paramsSpecifiers = visitor.getSpecifiersList();
        for (SpecifiersSet specifiersSet : paramsSpecifiers) {
            checkInstanceParamSpecifiers(specifiersSet);
        }
    }

    private static void checkInstanceParamSpecifiers(SpecifiersSet specifiers) {
        final boolean isNorace = specifiers.contains(NonTypeSpecifier.NORACE);

        if (isNorace && specifiers.size() > 1 || !isNorace && !specifiers.isEmpty()) {
            specifiers.emitError(FMT_ERR_INSTANCE_PARAM_NORACE_OTHER);
        } else if (isNorace && specifiers.sizeWithRepetitions() > 1) {
            specifiers.emitRepetitionWarnings();
        }
    }

    /**
     * Simple utility method for determining minimum.
     */
    private static <T extends Comparable<T>> T min(T value1, T value2) {
        return   value1.compareTo(value2) < 0
                ? value1
                : value2;
    }

    /**
     * Simple method for finding maximum.
     */
    private static <T extends Comparable<T>> T max(T value1, T value2) {
        return   value1.compareTo(value2) > 0
               ? value1
               : value2;
    }

    /**
     * Class whose objects are responsible for processing the non-type
     * specifiers and providing convenient access to them.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class SpecifiersSet {
        /**
         * Object that will be notified about detected errors.
         */
        private final ErrorHelper errorHelper;

        /**
         * Fields with actual data about specifiers.
         */
        private final ImmutableMap<NonTypeSpecifier, Interval> specifiers;
        private final ImmutableList<LocationsPin<NonTypeSpecifier>> repeatedSpecifiers;
        private final Optional<Interval> maybeInterval;

        /**
         * Collects the information about specifiers from given list. Does not
         * emit any error or warning. For such operation use other methods of
         * this class.
         */
        public SpecifiersSet(List<TypeElement> typeElements, ErrorHelper errorHelper) {
            checkNotNull(typeElements, "type elements cannot be null");
            checkNotNull(errorHelper, "error helper cannot be null");

            final SpecifiersVisitor visitor = new SpecifiersVisitor();
            for (TypeElement typeElement : typeElements) {
                typeElement.accept(visitor, null);
            }

            this.errorHelper = errorHelper;
            this.specifiers = visitor.getSpecifiers();
            this.repeatedSpecifiers = visitor.getRepeatedSpecifiers();
            this.maybeInterval = visitor.getInterval();
        }

        /**
         * @return Interval of the specifiers contained in this set. It is
         *         absent if it could have not been determined.
         */
        public Optional<Interval> getInterval() {
            return maybeInterval;
        }

        /**
         * @return <code>true</code> if and only if given object is not null and
         *         it is of class <code>NonTypeSpecifier</code> and is contained
         *         in this set.
         */
        public boolean contains(Object object) {
            if (object == null || NonTypeSpecifier.class != object.getClass()) {
                return false;
            }

            return specifiers.containsKey(object);
        }

        /**
         * @return Count of unique specifiers contained in this set.
         */
        public int size() {
            return specifiers.size();
        }

        /**
         * @return Count of all non-type specifiers that were processed.
         */
        public int sizeWithRepetitions() {
            return specifiers.size() + repeatedSpecifiers.size();
        }

        /**
         * @return <code>true</code> if and only if this set contains no
         *         specifiers.
         */
        public boolean isEmpty() {
            return specifiers.isEmpty();
        }

        /**
         * Emits warnings about encountered repetitions to the error helper
         * given at the construction.
         */
        public void emitRepetitionWarnings() {
            for (LocationsPin<NonTypeSpecifier> repetition : repeatedSpecifiers) {
                errorHelper.warning(
                        repetition.getLocation(),
                        Optional.of(repetition.getEndLocation()),
                        format(FMT_ERR_SPECIFIER_REPEATED, repetition.get().getRid().getName())
                );
            }
        }

        /**
         * Emits the given error message to the error helper given at
         * construction. The whole interval of specifiers is marked as invalid.
         *
         * @throws NullPointerException Given argument is null.
         * @throws IllegalStateException The interval is not present.
         */
        public void emitError(String errMsg) {
            checkNotNull(errMsg, "the error message cannot be null");
            checkState(maybeInterval.isPresent(), "the specifiers interval is absent");

            final Interval interval = maybeInterval.get();
            errorHelper.error(
                    interval.getLocation(),
                    interval.getEndLocation(),
                    errMsg
            );
        }
    }

    /**
     * Visitor that collects information about encountered non-type specifiers.
     * It expects only objects that are subclasses of <code>TypeElement</code>.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class SpecifiersVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Variables that will contain the information from specifiers.
         */
        private final Map<NonTypeSpecifier, Interval> specifiers = new HashMap<>();
        private final List<LocationsPin<NonTypeSpecifier>> repeatedSpecifiers = new ArrayList<>();
        private Optional<Location> startLocation = Optional.absent();
        private Optional<Location> endLocation = Optional.absent();

        private ImmutableMap<NonTypeSpecifier, Interval> getSpecifiers() {
            return ImmutableMap.copyOf(specifiers);
        }

        private ImmutableList<LocationsPin<NonTypeSpecifier>> getRepeatedSpecifiers() {
            return ImmutableList.copyOf(repeatedSpecifiers);
        }

        private Optional<Interval> getInterval() {
            return   startLocation.isPresent() && endLocation.isPresent()
                   ? Optional.of(new Interval(startLocation.get(), endLocation.get()))
                   : Optional.<Interval>absent();
        }

        @Override
        public Void visitRid(Rid rid, Void arg) {
            processSpecifier(rid.getId(), rid.getLocation(), rid.getEndLocation());
            return null;
        }

        @Override
        public Void visitQualifier(Qualifier qualifier, Void arg) {
            processSpecifier(qualifier.getId(), qualifier.getLocation(), qualifier.getEndLocation());
            return null;
        }

        @Override
        public Void visitTypeElement(TypeElement typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitTypeofType(TypeofType typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitTypeofExpr(TypeofExpr typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitTypename(Typename typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitComponentTyperef(ComponentTyperef typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitTagRef(TagRef typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitAttributeRef(AttributeRef typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitStructRef(StructRef typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitEnumRef(EnumRef typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitAttribute(Attribute typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitGccAttribute(GccAttribute typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitTargetAttribute(TargetAttribute typeElement, Void arg) {
            return null;
        }

        @Override
        public Void visitNescAttribute(NescAttribute typeElement, Void arg) {
            return null;
        }

        private void processSpecifier(RID rid, Location startLoc, Location endLoc) {
            // Check if it is a non-type specifier and find the mapping
            final NonTypeSpecifier specifier = RID_MAP.get(rid);
            if (specifier == null) {
                return;
            }

            // Add the new specifier
            if (specifiers.containsKey(specifier)) {
                repeatedSpecifiers.add(new LocationsPin<>(specifier, startLoc, endLoc));
            } else {
                specifiers.put(specifier, new Interval(startLoc, endLoc));
            }

            // Update locations
            startLocation =   startLocation.isPresent()
                            ? Optional.of(min(startLocation.get(), startLoc))
                            : Optional.of(startLoc);
            endLocation =   endLocation.isPresent()
                          ? Optional.of(max(endLocation.get(), endLoc))
                          : Optional.of(endLoc);
        }
    }

    /**
     * A visitor that collects information about non-type specifiers from
     * interface instance parameters.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class InstanceParametersVisitor extends ExceptionVisitor<Void, Void> {
        private final ErrorHelper errorHelper;

        /**
         * Builder that will create the immutable list with specifiers set.
         */
        private final ImmutableList.Builder<SpecifiersSet> listBuilder = ImmutableList.builder();

        private InstanceParametersVisitor(ErrorHelper errorHelper) {
            this.errorHelper = errorHelper;
        }

        private ImmutableList<SpecifiersSet> getSpecifiersList() {
            return listBuilder.build();
        }

        @Override
        public Void visitDataDecl(DataDecl dataDecl, Void arg) {
            listBuilder.add(new SpecifiersSet(dataDecl.getModifiers(), errorHelper));
            return null;
        }

        @Override
        public Void visitErrorDecl(ErrorDecl errDecl, Void arg) {
            return null;
        }
    }

    /**
     * Enumeration type that represents a non-type specifier. It is defined to
     * ensure that type specifiers from <code>RID</code> enumeration type are
     * not used.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum NonTypeSpecifier {
        /* original C storage-class specifiers */
        TYPEDEF(RID.TYPEDEF),
        EXTERN(RID.EXTERN),
        STATIC(RID.STATIC),
        AUTO(RID.AUTO),
        REGISTER(RID.REGISTER),

        /* NesC storage-class specifiers */
        COMMAND(RID.COMMAND),
        EVENT(RID.EVENT),
        ASYNC(RID.ASYNC),
        TASK(RID.TASK),
        NORACE(RID.NORACE),

        /* original and NesC function specifiers */
        INLINE(RID.INLINE),
        DEFAULT(RID.DEFAULT);

        private final RID rid;

        private NonTypeSpecifier(RID rid) {
            this.rid = rid;
        }

        public RID getRid() {
            return rid;
        }
    }
}
