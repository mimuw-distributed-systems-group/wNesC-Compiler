package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.common.util.VariousUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Object that counts inline, banked and non-banked functions in the final
 * program.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class FunctionsCounter {
    /**
     * Declarations partition of a program whose function will be counted.
     */
    private final DeclarationsPartitioner.Partition declsPartition;

    /**
     * Set with names of functions that have been already counted, i.e. their
     * definition have been encountered.
     */
    private final Set<String> countedFunctions;

    /**
     * Count of inline functions in the program.
     */
    private int countInlineFunctions;

    /**
     * Count of banked functions in the program.
     */
    private int countBankedFunctions;

    /**
     * Count of non-banked functions in the program.
     */
    private int countNonbankedFunctions;

    /**
     * Value indicating if the functions have been already counted.
     */
    private boolean countingDone;

    FunctionsCounter(DeclarationsPartitioner.Partition declsPartition) {
        checkNotNull(declsPartition, "declarations partition cannot be null");
        this.declsPartition = declsPartition;
        this.countedFunctions = new HashSet<>();
        this.countNonbankedFunctions = this.countBankedFunctions = this.countInlineFunctions = 0;
        this.countingDone = false;
    }

    /**
     * Count functions in the declarations partition. Each function that is
     * defined is classified as inline, banked or non-banked and counted.
     */
    void count() {
        if (countingDone) {
            return;
        }

        countingDone = true;

        final FunctionEnvironment headerEnvironment = analyzeHeader();

        for (String bankName : declsPartition.getCodeFiles().keySet()) {
            final FunctionEnvironment bankEnvironment = new FunctionEnvironment(
                    headerEnvironment.firstBankedFuns, headerEnvironment.firstInlineFuns,
                    headerEnvironment.firstNotInlineFuns);
            for (Declaration bankDeclaration : declsPartition.getCodeFiles().get(bankName)) {
                if (bankDeclaration instanceof FunctionDecl) {
                    bankEnvironment.accumulate((FunctionDecl) bankDeclaration);
                } else if (bankDeclaration instanceof DataDecl) {
                    bankEnvironment.accumulate((DataDecl) bankDeclaration);
                }
            }
        }
    }

    private FunctionEnvironment analyzeHeader() {
        final FunctionEnvironment functionEnvironment = new FunctionEnvironment();

        for (Declaration declaration : declsPartition.getHeaderFile()) {
            if (declaration instanceof FunctionDecl) {
                functionEnvironment.accumulate((FunctionDecl) declaration);
            } else if (declaration instanceof DataDecl) {
                functionEnvironment.accumulate((DataDecl) declaration);
            }
        }

        return functionEnvironment;
    }

    /**
     * Get the count of inline functions. This method cannot be called before
     * {@link FunctionsCounter#count} is called.
     *
     * @return Count of inline functions.
     * @throws IllegalStateException The counting has not been performed yet,
     *                               i.e. method {@link FunctionsCounter#count}
     *                               has not been called yet.
     */
    int getInlineFunctionsCount() {
        checkState(countingDone, "cannot get the count before the counting operation");
        return countInlineFunctions;
    }

    /**
     * Get the count of banked functions. This method cannot be called before
     * {@link FunctionsCounter#count} is called.
     *
     * @return Count of banked functions.
     * @throws IllegalStateException The counting has not been performed yet,
     *                               i.e. method {@link FunctionsCounter#count}
     *                               has not been called yet.
     */
    int getBankedFunctionsCount() {
        checkState(countingDone, "cannot get the count before the counting operation");
        return countBankedFunctions;
    }

    /**
     * Get the count of non-banked functions. This method cannot be called
     * before {@link FunctionsCounter#count} is called.
     *
     * @return Count of non-banked functions.
     * @throws IllegalStateException The counting has not been performed yet,
     *                               i.e. method {@link FunctionsCounter#count}
     *                               has not been called yet.
     */
    int getNonbankedFunctionsCount() {
        checkState(countingDone, "cannot get the count before the counting operation");
        return countNonbankedFunctions;
    }

    /**
     * Helper class that facilitates analysis of functions declarations.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class FunctionEnvironment {
        /**
         * First set with unique names of functions declared at least once as
         * banked.
         */
        private final Set<String> firstBankedFuns;

        /**
         * First set of unique names of functions declared at least once as
         * inline.
         */
        private final Set<String> firstInlineFuns;

        /**
         * First set of unique names of functions declared at least once as
         * non-inline, i.e. there exists at least one declaration without
         * <code>inline</code> keyword for a function from this set.
         */
        private final Set<String> firstNotInlineFuns;

        /**
         * Second set with unique names of functions declared at least once as
         * banked.
         */
        private final Optional<Set<String>> secondBankedFuns;

        /**
         * Second set of unique names of functions declared at least once as
         * inline.
         */
        private final Optional<Set<String>> secondInlineFuns;

        /**
         * Second set of unique names of functions declared at least once as
         * non-inline, i.e. there exists at least one declaration without
         * <code>inline</code> keyword for a function from this set.
         */
        private final Optional<Set<String>> secondNotInlineFuns;

        private FunctionEnvironment() {
            this.firstBankedFuns = new HashSet<>();
            this.firstInlineFuns = new HashSet<>();
            this.firstNotInlineFuns = new HashSet<>();
            this.secondBankedFuns = Optional.absent();
            this.secondInlineFuns = Optional.absent();
            this.secondNotInlineFuns = Optional.absent();
        }

        private FunctionEnvironment(Set<String> bankedFuns, Set<String> inlineFuns,
                    Set<String> notInlineFuns) {
            checkNotNull(bankedFuns, "banked functions cannot be null");
            checkNotNull(inlineFuns, "inline functions cannot be null");
            checkNotNull(notInlineFuns, "set with functions that are not inline cannot be null");
            this.firstBankedFuns = new HashSet<>();
            this.firstInlineFuns = new HashSet<>();
            this.firstNotInlineFuns = new HashSet<>();
            this.secondBankedFuns = Optional.of(bankedFuns);
            this.secondInlineFuns = Optional.of(inlineFuns);
            this.secondNotInlineFuns = Optional.of(notInlineFuns);
        }

        private void accumulate(FunctionDecl functionDecl) {
            final String funUniqueName = DeclaratorUtils.getUniqueName(
                    functionDecl.getDeclarator()).get();
            final boolean isBanked = DeclaratorUtils.getIsBanked(functionDecl.getDeclarator());
            final EnumSet<RID> rids = TypeElementUtils.collectRID(functionDecl.getModifiers());

            if (!countedFunctions.add(funUniqueName)) {
                throw new RuntimeException("function '" + funUniqueName + "' defined more than once");
            }

            if (isBanked || containsBankedFunction(funUniqueName)) {
                ++countBankedFunctions;
            } else if (rids.contains(RID.INLINE)) {
                ++countInlineFunctions;
            } else {
                ++countNonbankedFunctions;
            }

            accumulate(funUniqueName, isBanked, rids);
        }

        private void accumulate(DataDecl dataDecl) {
            if (dataDecl.getDeclarations().size() > 1) {
                throw new RuntimeException("unexpected unseparated declarations");
            } else if (dataDecl.getDeclarations().isEmpty()) {
                return;
            }

            final VariableDecl variableDecl = (VariableDecl) dataDecl.getDeclarations().getFirst();
            final Optional<NestedDeclarator> deepestNestedDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(variableDecl.getDeclarator().get());

            if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof FunctionDeclarator) {
                // Forward declaration of a function
                final FunctionDeclarator functionDeclarator =
                        (FunctionDeclarator) deepestNestedDeclarator.get();
                accumulate(DeclaratorUtils.getUniqueName(deepestNestedDeclarator.get()).get(),
                        VariousUtils.getBooleanValue(functionDeclarator.getIsBanked()),
                        TypeElementUtils.collectRID(dataDecl.getModifiers()));
            }
        }

        private void accumulate(String funUniqueName, boolean isBanked, Set<RID> rids) {
            if (isBanked) {
                if (rids.contains(RID.INLINE) || containsInlineFunction(funUniqueName)) {
                    throw new RuntimeException("banked function '" + funUniqueName
                            + "' is declared as inline");
                }
                addBankedFunction(funUniqueName);
            } else if (rids.contains(RID.INLINE)) {
                if (!rids.contains(RID.STATIC)) {
                    throw new RuntimeException("inline function '" + funUniqueName
                            + "' is not declared as static");
                } else if (containsBankedFunction(funUniqueName)) {
                    throw new RuntimeException("inline function '" + funUniqueName
                            + "' is declared as banked");
                } else if (containsNotInlineFunction(funUniqueName)) {
                    throw new RuntimeException("inline function '" + funUniqueName
                            + "' has not been previously declared as inline");
                }
                addInlineFunction(funUniqueName);
            } else {
                if (containsInlineFunction(funUniqueName)) {
                    throw new RuntimeException("function '" + funUniqueName
                            + "' has been previously declared as inline");
                }
                addNotInlineFunction(funUniqueName);
            }
        }

        private boolean containsBankedFunction(String funUniqueName) {
            return secondBankedFuns.isPresent() && secondBankedFuns.get().contains(funUniqueName)
                || firstBankedFuns.contains(funUniqueName);
        }

        private boolean containsInlineFunction(String funUniqueName) {
            return secondInlineFuns.isPresent() && secondInlineFuns.get().contains(funUniqueName)
                    || firstInlineFuns.contains(funUniqueName);
        }

        private boolean containsNotInlineFunction(String funUniqueName) {
            return secondNotInlineFuns.isPresent() && secondNotInlineFuns.get().contains(funUniqueName)
                    || firstNotInlineFuns.contains(funUniqueName);
        }

        private void addBankedFunction(String funUniqueName) {
            firstBankedFuns.add(funUniqueName);
        }

        private void addInlineFunction(String funUniqueName) {
            firstInlineFuns.add(funUniqueName);
        }

        private void addNotInlineFunction(String funUniqueName) {
            firstNotInlineFuns.add(funUniqueName);
        }
    }
}
