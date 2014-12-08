package pl.edu.mimuw.nesc.analysis;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import pl.edu.mimuw.nesc.ast.util.Interval;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.util.LocationsPin;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.object.Linkage;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.ScopeType;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.concat;
import static pl.edu.mimuw.nesc.problem.issue.InvalidGenericParamSpecifiersError.InvalidCombinationType.*;

/**
 * A class with definitions that are intended to perform the analysis of
 * storage-class and function specifiers.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class SpecifiersAnalysis {
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
     * Set with all main specifiers.
     * @see SpecifiersSet#firstMainSpecifier()
     */
    private static final ImmutableSet<NonTypeSpecifier> MAIN_SPECIFIERS = ImmutableSet.of(
        NonTypeSpecifier.TYPEDEF,
        NonTypeSpecifier.EXTERN,
        NonTypeSpecifier.STATIC,
        NonTypeSpecifier.REGISTER,
        NonTypeSpecifier.AUTO,
        NonTypeSpecifier.COMMAND,
        NonTypeSpecifier.EVENT,
        NonTypeSpecifier.TASK
    );

    /**
     * Function that transforms a specifiers map entry to an equivalent object
     * of class <code>LocationsPin</code>.
     */
    private static final Function<Map.Entry<NonTypeSpecifier, Interval>, LocationsPin<NonTypeSpecifier>>
            ENTRY_TO_PIN_FUNCTION = new Function<Map.Entry<NonTypeSpecifier, Interval>, LocationsPin<NonTypeSpecifier>>() {
        @Override
        public LocationsPin<NonTypeSpecifier> apply(Map.Entry<NonTypeSpecifier, Interval> entry) {
            checkNotNull(entry, "the entry cannot be null");
            final Interval interval = entry.getValue();
            return LocationsPin.of(entry.getKey(), interval.getLocation(), interval.getEndLocation());
        }
    };

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
                    new InvalidGenericParamSpecifiersError(TYPEDEF_WITH_OTHER)
            );
        } else if (isTypedef && specifiers.sizeWithRepetitions() > 1) {
            specifiers.emitRepetitionWarnings();
        } else if (!isTypedef && isNorace && specifiers.size() > 1
                   || !isTypedef && !isNorace && !specifiers.isEmpty()) {
            specifiers.emitError(new InvalidGenericParamSpecifiersError(NORACE_WITH_OTHER));
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
            specifiers.emitError(new InvalidInstanceParamSpecifiersError());
        } else if (isNorace && specifiers.sizeWithRepetitions() > 1) {
            specifiers.emitRepetitionWarnings();
        }
    }

    /**
     * Determines the linkage of the given identifier. The environment shouldn't
     * have acknowledged the declaration of the identifier that the linkage
     * is determined for.
     *
     * @param identifier Identifier that the linkage will be determined for.
     * @param environment Current environment.
     * @param mainSpecifier Main specifier from the declaration of the
     *                      identifier (an absent value means lack of the main
     *                      specifier in the declaration).
     * @param type Type of the identifier.
     * @return The linkage of the identifier if it can be determined. Otherwise,
     *         absent value.
     * @throws NullPointerException One of the arguments is null.
     */
    public static Optional<Linkage> determineLinkage(String identifier, Environment environment,
            Optional<NonTypeSpecifier> mainSpecifier, Type type) {
        // Check parameters
        checkNotNull(identifier, "the identifier cannot be null");
        checkNotNull(environment, "the environment cannot be null");
        checkNotNull(mainSpecifier, "the main specifier cannot be null");
        checkNotNull(type, "the type cannot be null");

        final ScopeType scopeType = environment.getScopeType();

        // No linkage
        if (scopeType == ScopeType.COMPOUND && (!mainSpecifier.isPresent()
                        || mainSpecifier.get() != NonTypeSpecifier.EXTERN)
                || !type.isObjectType() && !type.isFunctionType()
                || scopeType.isParameterScope()) {
            return Optional.of(Linkage.NONE);
        }

        // FIXME implement other cases
        return Optional.absent();
    }

    /**
     * Class whose objects are responsible for processing the non-type
     * specifiers and providing convenient access to them.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class SpecifiersSet {
        /**
         * Predicate that allows testing if a non-type specifier is a main
         * specifier.
         */
        private static final Predicate<LocationsPin<NonTypeSpecifier>> mainSpecifierPredicate =
                new Predicate<LocationsPin<NonTypeSpecifier>> () {
            @Override
            public boolean apply(LocationsPin<NonTypeSpecifier> specifier) {
                checkNotNull(specifier, "the specifier cannot be null");
                return MAIN_SPECIFIERS.contains(specifier.get());
            }
        };

        /**
         * Object that will be notified about detected errors.
         */
        private final ErrorHelper errorHelper;

        /**
         * Fields with actual data about specifiers.
         */
        private final ImmutableMap<NonTypeSpecifier, Interval> firstSpecifiers;
        private final ImmutableMap<NonTypeSpecifier, Interval> mainSpecifiers;
        private final ImmutableList<LocationsPin<NonTypeSpecifier>> repeatedSpecifiers;
        private final Optional<NonTypeSpecifier> firstMainSpecifier;
        private final Optional<Interval> maybeInterval;

        /**
         * Predicate for comparing a specifier with the main one.
         */
        private final Predicate<LocationsPin<NonTypeSpecifier>> otherThanMainPredicate =
                new Predicate<LocationsPin<NonTypeSpecifier>>() {
            @Override
            public boolean apply(LocationsPin<NonTypeSpecifier> specifier) {
                checkNotNull(specifier, "the specifier cannot be null");
                return specifier.get() != firstMainSpecifier.orNull();
            }
        };

        /**
         * Constructs an empty specifiers set.
         */
        public SpecifiersSet(ErrorHelper errorHelper) {
            checkNotNull(errorHelper, "error helper cannot be null");

            this.firstSpecifiers = ImmutableMap.of();
            this.mainSpecifiers = ImmutableMap.of();
            this.repeatedSpecifiers = ImmutableList.of();
            this.firstMainSpecifier = Optional.absent();
            this.maybeInterval = Optional.absent();
            this.errorHelper = errorHelper;
        }

        /**
         * Collects the information about specifiers from given list. Does not
         * emit any error or warning. For such operation use other methods of
         * this class.
         */
        public SpecifiersSet(List<TypeElement> typeElements, ErrorHelper errorHelper) {
            checkNotNull(typeElements, "type elements cannot be null");
            checkNotNull(errorHelper, "error helper cannot be null");

            // Visit all specifiers in order of their appearance
            final SpecifiersVisitor visitor = new SpecifiersVisitor();
            for (TypeElement typeElement : typeElements) {
                typeElement.accept(visitor, null);
            }

            // Build this object
            final Builder builder = new Builder(visitor);
            this.firstSpecifiers = builder.buildFirstSpecifiers();
            this.mainSpecifiers = builder.buildMainSpecifiers();
            this.repeatedSpecifiers = builder.buildRepeatedSpecifiers();
            this.firstMainSpecifier = builder.buildFirstMainSpecifier();
            this.maybeInterval = builder.buildInterval();
            this.errorHelper = errorHelper;
        }

        /**
         * @return Interval of the specifiers contained in this set. It is
         *         absent if it could have not been determined.
         */
        public Optional<Interval> getInterval() {
            return maybeInterval;
        }

        /**
         * @return <code>true</code> if and only if given object is contained
         *         in this set.
         */
        public boolean contains(Object object) {
            return firstSpecifiers.containsKey(object);
        }

        /**
         * @return Count of unique specifiers contained in this set.
         */
        public int size() {
            return firstSpecifiers.size();
        }

        /**
         * @return Count of all non-type specifiers that were processed.
         */
        public int sizeWithRepetitions() {
            return firstSpecifiers.size() + repeatedSpecifiers.size();
        }

        /**
         * @return <code>true</code> if and only if this set contains no
         *         specifiers.
         */
        public boolean isEmpty() {
            return firstSpecifiers.isEmpty();
        }

        /**
         * Emits warnings about encountered repetitions to the error helper
         * given at the construction.
         */
        public void emitRepetitionWarnings() {
            emitRepetitionWarnings(Optional.<Set<NonTypeSpecifier>>absent());
        }

        /**
         * Emits warnings for repetitions of specifiers from a set.
         *
         * @param warnFor The set of specifiers that the warnings about
         *                repetitions will be emitted for. If absent, warnings
         *                will be emitted for all repeated specifiers.
         */
        public void emitRepetitionWarnings(Optional<Set<NonTypeSpecifier>> warnFor) {
            for (LocationsPin<NonTypeSpecifier> repetition : repeatedSpecifiers) {
                if (warnFor.isPresent() && !warnFor.get().contains(repetition.get())) {
                    continue;
                }

                errorHelper.warning(
                        repetition.getLocation(),
                        repetition.getEndLocation(),
                        new NonTypeSpecifierRepetitionWarning(repetition.get().getRid())
                );
            }
        }

        /**
         * Emits the given error to the error helper given at construction. The
         * whole interval of specifiers is marked as invalid.
         *
         * @throws NullPointerException Given argument is null.
         * @throws IllegalStateException The interval is not present.
         */
        public void emitError(ErroneousIssue error) {
            checkNotNull(error, "the error cannot be null");
            checkState(maybeInterval.isPresent(), "the specifiers interval is absent");

            final Interval interval = maybeInterval.get();
            errorHelper.error(
                    interval.getLocation(),
                    interval.getEndLocation(),
                    error
            );
        }

        /**
         * Get the set with all main specifiers contained in this set.
         *
         * @return Immutable set with main specifiers.
         */
        public ImmutableSet<NonTypeSpecifier> getMainSpecifiers() {
            return mainSpecifiers.keySet();
        }

        /**
         * Validates main specifiers and emits errors if the validation fails.
         *
         * @return <code>true</code> if and only if the main specifiers are
         *         correctly used.
         */
        public boolean validateMainSpecifiers() {
            // Handle the correct cases
            emitRepetitionWarnings(Optional.of(firstMainSpecifier.asSet()));
            if (mainSpecifiers.size() <= 1) {
                return true;
            }

            // Handle the invalid case
            checkState(firstMainSpecifier.isPresent(), "the main specifier unexpectedly absent");
            final FluentIterable<LocationsPin<NonTypeSpecifier>> mainSpecsIt =
                    FluentIterable.from(mainSpecifiers.entrySet())
                                  .transform(ENTRY_TO_PIN_FUNCTION);
            final FluentIterable<LocationsPin<NonTypeSpecifier>> invalidSpecs =
                    FluentIterable.from(concat(mainSpecsIt, repeatedSpecifiers))
                                  .filter(otherThanMainPredicate)
                                  .filter(mainSpecifierPredicate);

            // Emit errors
            for (LocationsPin<NonTypeSpecifier> invalidSpec : invalidSpecs) {
                errorHelper.error(
                        invalidSpec.getLocation(),
                        invalidSpec.getEndLocation(),
                        new ConflictingStorageSpecifierError(invalidSpec.get().getRid(),
                                firstMainSpecifier.get().getRid())
                );
            }

            return false;
        }

        /**
         * Check if the main specifiers are correctly used, i.e. there is at
         * most one main specifier in this set.
         *
         * @return <code>true</code> if and only if the main specifiers are
         *         correctly used.
         */
        public boolean goodMainSpecifier() {
            return mainSpecifiers.size() <= 1;
        }

        /**
         * <p>A main specifier is the storage-class specifier used in
         * a declaration. It makes sense because only one storage-class
         * specifier can be used. It is one of the following specifiers:
         * </p>
         * <ul>
         *     <li><code>typedef</code></li>
         *     <li><code>static</code></li>
         *     <li><code>extern</code></li>
         *     <li><code>register</code></li>
         *     <li><code>auto</code></li>
         *     <li><code>command</code></li>
         *     <li><code>event</code></li>
         *     <li><code>task</code></li>
         * </ul>
         *
         * @return The main specifier from this set with the smallest start
         *         location. If it does not contain any main specifiers, then
         *         the value is absent.
         */
        public Optional<NonTypeSpecifier> firstMainSpecifier() {
            return firstMainSpecifier;
        }

        /**
         * A class that is responsible for constructing individual elements of
         * a specifiers set.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private static class Builder {
            private final SpecifiersVisitor visitor;
            private final ImmutableMap<NonTypeSpecifier, Interval> firstSpecifiers;

            private Builder(SpecifiersVisitor visitor) {
                checkNotNull(visitor, "the visitor cannot be null");

                this.firstSpecifiers = visitor.getSpecifiers();
                this.visitor = visitor;
            }

            private ImmutableMap<NonTypeSpecifier, Interval> buildFirstSpecifiers() {
                return firstSpecifiers;
            }

            private ImmutableList<LocationsPin<NonTypeSpecifier>> buildRepeatedSpecifiers() {
                return visitor.getRepeatedSpecifiers();
            }

            private ImmutableMap<NonTypeSpecifier, Interval> buildMainSpecifiers() {
                return ImmutableMap.copyOf(Maps.filterKeys(firstSpecifiers,
                                           Predicates.in(MAIN_SPECIFIERS)));
            }

            private Optional<NonTypeSpecifier> buildFirstMainSpecifier() {
                return FluentIterable.from(firstSpecifiers.keySet())
                        .firstMatch(Predicates.in(MAIN_SPECIFIERS));
            }

            private Optional<Interval> buildInterval() {
                return visitor.getInterval();
            }
        }
    }

    /**
     * Visitor that collects information about encountered non-type specifiers.
     * It expects only objects that are subclasses of <code>TypeElement</code>.
     * This visitor shall visit the elements in order they appear in the
     * program.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class SpecifiersVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Variables that will contain the information from specifiers.
         */
        private final Set<NonTypeSpecifier> visitedSpecifiers = EnumSet.noneOf(NonTypeSpecifier.class);
        private final ImmutableMap.Builder<NonTypeSpecifier, Interval> specifiersBuilder = ImmutableMap.builder();
        private final ImmutableList.Builder<LocationsPin<NonTypeSpecifier>> repeatedSpecifiersBuilder = ImmutableList.builder();
        private final Interval.Builder intervalBuilder = Interval.builder();

        private ImmutableMap<NonTypeSpecifier, Interval> getSpecifiers() {
            return specifiersBuilder.build();
        }

        private ImmutableList<LocationsPin<NonTypeSpecifier>> getRepeatedSpecifiers() {
            return repeatedSpecifiersBuilder.build();
        }

        private Optional<Interval> getInterval() {
            return intervalBuilder.tryBuild();
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
            if (visitedSpecifiers.contains(specifier)) {
                repeatedSpecifiersBuilder.add(LocationsPin.of(specifier, startLoc, endLoc));
            } else {
                specifiersBuilder.put(specifier, Interval.of(startLoc, endLoc));
                visitedSpecifiers.add(specifier);
            }

            // Update locations
            intervalBuilder.initStartLocation(startLoc)
                    .endLocation(endLoc);
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

    /**
     * Prevent this class from being instantiated.
     */
    private SpecifiersAnalysis() {
    }
}
