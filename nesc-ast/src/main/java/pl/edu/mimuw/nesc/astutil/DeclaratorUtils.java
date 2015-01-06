package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Common operations on declarators.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class DeclaratorUtils {

    private static final DeclaratorNameVisitor DECLARATOR_NAME_VISITOR = new DeclaratorNameVisitor();
    private static final IsFunctionDeclaratorVisitor IS_FUNCTION_DECLARATOR_VISITOR = new IsFunctionDeclaratorVisitor();
    private static final FunctionDeclaratorExtractor FUNCTION_DECLARATOR_EXTRACTOR = new FunctionDeclaratorExtractor();
    private static final IdentifierIntervalVisitor IDENTIFIER_INTERVAL_VISITOR = new IdentifierIntervalVisitor();
    private static final SetUniqueNameVisitor SET_UNIQUE_NAME_VISITOR = new SetUniqueNameVisitor();
    private static final DeepestNestedDeclaratorVisitor DEEPEST_NESTED_DECLARATOR_VISITOR = new DeepestNestedDeclaratorVisitor();

    /**
     * Gets declarator's name.
     *
     * @param declarator declarator
     * @return declarator's name
     */
    public static Optional<String> getDeclaratorName(Declarator declarator) {
        checkNotNull(declarator, "declarator cannot be null");
        return declarator.accept(DECLARATOR_NAME_VISITOR, null);
    }

    public static boolean isFunctionDeclarator(Declarator declarator) {
        checkNotNull(declarator, "declarator cannot be null");
        return declarator.accept(IS_FUNCTION_DECLARATOR_VISITOR, null);
    }

    public static FunctionDeclarator getFunctionDeclarator(Declarator declarator) {
        checkNotNull(declarator, "declarator cannot be null");
        return declarator.accept(FUNCTION_DECLARATOR_EXTRACTOR, null);
    }

    /**
     * @param declarator Declarator to be traversed.
     * @return The interval that only the identifier is contained in. If the
     * identifier is not present, the value is absent.
     */
    public static Optional<Interval> getIdentifierInterval(Declarator declarator) {
        checkNotNull(declarator, "the declarator cannot be null");
        return declarator.accept(IDENTIFIER_INTERVAL_VISITOR, null);
    }

    /**
     * <p>Mangles the name found in the given declarator and saves the name
     * after mangling in it.</p>
     *
     * @param declarator Declarator to traverse.
     * @param mangleFun Function that returns mangled names.
     * @return Name after mangling. The object is present if the given
     *         declarator contains an identifier declarator.
     */
    public static Optional<String> mangleDeclaratorName(Declarator declarator,
            Function<String, String> mangleFun) {

        checkNotNull(declarator, "declarator cannot be null");
        checkNotNull(mangleFun, "the mangling function cannot be null");

        final MangleNameVisitor visitor = new MangleNameVisitor(mangleFun);
        return declarator.accept(visitor, null);
    }

    /**
     * <p>Sets the unique name in the given declarator to the given one. If no
     * identifier declarator is found, then calling this method has no effect.
     * </p>
     *
     * @param declarator Declarator with the unique name to change.
     * @param nameToSet Name to set in the identifier declarator nested in the
     *                  given one.
     */
    public static void setUniqueName(Declarator declarator, Optional<String> nameToSet) {
        checkNotNull(nameToSet, "name to set in the declarator cannot be null");
        declarator.accept(SET_UNIQUE_NAME_VISITOR, nameToSet);
    }

    /**
     * Get the deepest nested declarator contained in the given declarator.
     *
     * @param declarator Declarator that contains the necessary nested
     *                   declarator.
     * @return The deepest nested declarator contained in the given one (or the
     *         argument itself if it does not have nested declarators). The
     *         object is absent if the given declarator is an identifier
     *         declarator.
     * @throws NullPointerException Declarator is <code>null</code>.
     */
    public static Optional<NestedDeclarator> getDeepestNestedDeclarator(Declarator declarator) {
        checkNotNull(declarator, "declarator cannot be null");
        return declarator.accept(DEEPEST_NESTED_DECLARATOR_VISITOR, null);
    }

    /**
     * Acts as {@link DeclaratorUtils#getDeepestNestedDeclarator} if the given
     * declarator is present. If not, the returned object is absent.
     *
     * @param declarator Declarator that contains the necessary nested
     *                   declarator.
     * @return If the declarator in the argument is absent, then the returned
     *         object is absent. Otherwise, the result is the same if
     *         {@link DeclaratorUtils#getDeepestNestedDeclarator} is invoked.
     * @throws NullPointerException Declarator is <code>null</code>.
     */
    public static Optional<NestedDeclarator> getDeepestNestedDeclarator(Optional<Declarator> declarator) {
        checkNotNull(declarator, "declarator cannot be null");
        return declarator.isPresent()
                ? getDeepestNestedDeclarator(declarator.get())
                : Optional.<NestedDeclarator>absent();
    }

    private DeclaratorUtils() {
    }

    /**
     * Visitor for extracting declarator's name.
     *
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    private static class DeclaratorNameVisitor extends ExceptionVisitor<Optional<String>, Void> {

        @Override
        public Optional<String> visitFunctionDeclarator(FunctionDeclarator elem, Void arg) {
            if (elem.getDeclarator().isPresent()) {
                return elem.getDeclarator().get().accept(this, null);
            }
            return Optional.absent();
        }

        @Override
        public Optional<String> visitPointerDeclarator(PointerDeclarator elem, Void arg) {
            if (elem.getDeclarator().isPresent()) {
                return elem.getDeclarator().get().accept(this, null);
            }
            return Optional.absent();
        }

        @Override
        public Optional<String> visitQualifiedDeclarator(QualifiedDeclarator elem, Void arg) {
            if (elem.getDeclarator().isPresent()) {
                return elem.getDeclarator().get().accept(this, null);
            }
            return Optional.absent();
        }

        @Override
        public Optional<String> visitArrayDeclarator(ArrayDeclarator elem, Void arg) {
            if (elem.getDeclarator().isPresent()) {
                return elem.getDeclarator().get().accept(this, null);
            }
            return Optional.absent();
        }

        @Override
        public Optional<String> visitIdentifierDeclarator(IdentifierDeclarator elem, Void arg) {
            return Optional.of(elem.getName());
        }

        @Override
        public Optional<String> visitInterfaceRefDeclarator(InterfaceRefDeclarator elem, Void arg) {
            return elem.getDeclarator().get().accept(this, null);
        }

    }

    private static class IsFunctionDeclaratorVisitor extends ExceptionVisitor<Boolean, Void> {

        @Override
        public Boolean visitFunctionDeclarator(FunctionDeclarator elem, Void arg) {
            return Boolean.TRUE;
        }

        @Override
        public Boolean visitPointerDeclarator(PointerDeclarator elem, Void arg) {
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitQualifiedDeclarator(QualifiedDeclarator elem, Void arg) {
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitArrayDeclarator(ArrayDeclarator elem, Void arg) {
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitIdentifierDeclarator(IdentifierDeclarator elem, Void arg) {
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitInterfaceRefDeclarator(InterfaceRefDeclarator elem, Void arg) {
            return Boolean.FALSE;
        }

    }

    private static class FunctionDeclaratorExtractor extends ExceptionVisitor<FunctionDeclarator, Void> {

        @Override
        public FunctionDeclarator visitFunctionDeclarator(FunctionDeclarator elem, Void arg) {
            return elem;
        }

        @Override
        public FunctionDeclarator visitPointerDeclarator(PointerDeclarator elem, Void arg) {
            return elem.getDeclarator().get().accept(this, null);
        }

        @Override
        public FunctionDeclarator visitQualifiedDeclarator(QualifiedDeclarator elem, Void arg) {
            return elem.getDeclarator().get().accept(this, null);
        }

        @Override
        public FunctionDeclarator visitArrayDeclarator(ArrayDeclarator elem, Void arg) {
            throw new IllegalStateException("Function declarator not found");
        }

        @Override
        public FunctionDeclarator visitIdentifierDeclarator(IdentifierDeclarator elem, Void arg) {
            throw new IllegalStateException("Function declarator not found");
        }

        @Override
        public FunctionDeclarator visitInterfaceRefDeclarator(InterfaceRefDeclarator elem, Void arg) {
            return elem.getDeclarator().get().accept(this, null);
        }
    }

    /**
     * A visitor that extracts the interval of an identifier in a declarator.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class IdentifierIntervalVisitor extends ExceptionVisitor<Optional<Interval>, Void> {
        @Override
        public Optional<Interval> visitIdentifierDeclarator(IdentifierDeclarator declarator, Void arg) {
            return Optional.of(Interval.of(declarator.getLocation(), declarator.getEndLocation()));
        }

        @Override
        public Optional<Interval> visitInterfaceRefDeclarator(InterfaceRefDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        @Override
        public Optional<Interval> visitArrayDeclarator(ArrayDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        @Override
        public Optional<Interval> visitQualifiedDeclarator(QualifiedDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        @Override
        public Optional<Interval> visitFunctionDeclarator(FunctionDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        @Override
        public Optional<Interval> visitPointerDeclarator(PointerDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        private Optional<Interval> jump(Optional<Declarator> nextDeclarator) {
            if (nextDeclarator.isPresent()) {
                return nextDeclarator.get().accept(this, null);
            }
            return Optional.absent();
        }
    }

    /**
     * Mangles the name found in the given declarator. Returns the unique name.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class MangleNameVisitor extends ExceptionVisitor<Optional<String>, Void> {
        private final Function<String, String> mangleFun;

        private MangleNameVisitor(Function<String, String> manglingFun) {
            this.mangleFun = manglingFun;
        }

        @Override
        public Optional<String> visitIdentifierDeclarator(IdentifierDeclarator declarator, Void arg) {
            final Optional<String> uniqueName = Optional.of(mangleFun.apply(declarator.getName()));
            declarator.setUniqueName(uniqueName);
            return uniqueName;
        }

        @Override
        public Optional<String> visitInterfaceRefDeclarator(InterfaceRefDeclarator declarator, Void arg) {
            if (!declarator.getDeclarator().isPresent()) {
                throw new RuntimeException("the declarator inside an interface reference declarator is absent");
            } else if (!(declarator.getDeclarator().get() instanceof IdentifierDeclarator)) {
                throw new RuntimeException("unexpected inner declarator of an interface reference declarator: "
                        + declarator.getDeclarator().get().getClass());
            }

            final IdentifierDeclarator innerDeclarator = (IdentifierDeclarator) declarator.getDeclarator().get();
            final String nameToMangle = format("%s__%s", declarator.getName().getName(), innerDeclarator.getName());
            final Optional<String> uniqueName = Optional.of(mangleFun.apply(nameToMangle));
            innerDeclarator.setUniqueName(uniqueName);

            return uniqueName;
        }

        @Override
        public Optional<String> visitArrayDeclarator(ArrayDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        @Override
        public Optional<String> visitQualifiedDeclarator(QualifiedDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        @Override
        public Optional<String> visitFunctionDeclarator(FunctionDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        @Override
        public Optional<String> visitPointerDeclarator(PointerDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator());
        }

        private Optional<String> jump(Optional<Declarator> nextDeclarator) {
            return nextDeclarator.isPresent()
                    ? nextDeclarator.get().accept(this, null)
                    : Optional.<String>absent();
        }
    }

    /**
     * Sets the unique name to the given one in the encountered identifier
     * declarator.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class SetUniqueNameVisitor extends ExceptionVisitor<Void, Optional<String>> {
        @Override
        public Void visitIdentifierDeclarator(IdentifierDeclarator declarator, Optional<String> arg) {
            declarator.setUniqueName(arg);
            return null;
        }

        @Override
        public Void visitInterfaceRefDeclarator(InterfaceRefDeclarator declarator, Optional<String> arg) {
            jump(declarator.getDeclarator(), arg);
            return null;
        }

        @Override
        public Void visitArrayDeclarator(ArrayDeclarator declarator, Optional<String> arg) {
            jump(declarator.getDeclarator(), arg);
            return null;
        }

        @Override
        public Void visitQualifiedDeclarator(QualifiedDeclarator declarator, Optional<String> arg) {
            jump(declarator.getDeclarator(), arg);
            return null;
        }

        @Override
        public Void visitFunctionDeclarator(FunctionDeclarator declarator, Optional<String> arg) {
            jump(declarator.getDeclarator(), arg);
            return null;
        }

        @Override
        public Void visitPointerDeclarator(PointerDeclarator declarator, Optional<String> arg) {
            jump(declarator.getDeclarator(), arg);
            return null;
        }

        private Void jump(Optional<Declarator> nextDeclarator, Optional<String> arg) {
            if (nextDeclarator.isPresent()) {
                nextDeclarator.get().accept(this, arg);
            }

            return null;
        }
    }

    /**
     * Visitor that returns the deepest nested declarator of a visited
     * declarator (if it exists).
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class DeepestNestedDeclaratorVisitor extends ExceptionVisitor<Optional<NestedDeclarator>, Void> {
        @Override
        public Optional<NestedDeclarator> visitIdentifierDeclarator(IdentifierDeclarator declarator, Void arg) {
            return Optional.absent();
        }

        @Override
        public Optional<NestedDeclarator> visitInterfaceRefDeclarator(InterfaceRefDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator()).or(Optional.of(declarator));
        }

        @Override
        public Optional<NestedDeclarator> visitArrayDeclarator(ArrayDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator()).or(Optional.of(declarator));
        }

        @Override
        public Optional<NestedDeclarator> visitQualifiedDeclarator(QualifiedDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator()).or(Optional.of(declarator));
        }

        @Override
        public Optional<NestedDeclarator> visitFunctionDeclarator(FunctionDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator()).or(Optional.of(declarator));
        }

        @Override
        public Optional<NestedDeclarator> visitPointerDeclarator(PointerDeclarator declarator, Void arg) {
            return jump(declarator.getDeclarator()).or(Optional.of(declarator));
        }

        private Optional<NestedDeclarator> jump(Optional<Declarator> nextDeclarator) {
            return nextDeclarator.isPresent()
                    ? nextDeclarator.get().accept(this, null)
                    : Optional.<NestedDeclarator>absent();
        }
    }

}
