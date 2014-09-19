package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Interval;
import pl.edu.mimuw.nesc.ast.gen.*;

import static com.google.common.base.Preconditions.checkNotNull;

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

    /**
     * Gets declarator's name.
     *
     * @param declarator declarator
     * @return declarator's name
     */
    public static String getDeclaratorName(Declarator declarator) {
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
     *         identifier is not present, the value is absent.
     */
    public static Optional<Interval> getIdentifierInterval(Declarator declarator) {
        checkNotNull(declarator, "the declarator cannot be null");
        return declarator.accept(IDENTIFIER_INTERVAL_VISITOR, null);
    }

    private DeclaratorUtils() {
    }

    /**
     * Visitor for extracting declarator's name.
     *
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    private static class DeclaratorNameVisitor extends ExceptionVisitor<String, Void> {

        @Override
        public String visitFunctionDeclarator(FunctionDeclarator elem, Void arg) {
            return elem.getDeclarator().get().accept(this, null);
        }

        @Override
        public String visitPointerDeclarator(PointerDeclarator elem, Void arg) {
            if (elem.getDeclarator().isPresent()) {
                return elem.getDeclarator().get().accept(this, null);
            }
            return null;
        }

        @Override
        public String visitQualifiedDeclarator(QualifiedDeclarator elem, Void arg) {
            if (elem.getDeclarator().isPresent()) {
                return elem.getDeclarator().get().accept(this, null);
            }
            return null;
        }

        @Override
        public String visitArrayDeclarator(ArrayDeclarator elem, Void arg) {
            if (elem.getDeclarator().isPresent()) {
                return elem.getDeclarator().get().accept(this, null);
            }
            return null;
        }

        @Override
        public String visitIdentifierDeclarator(IdentifierDeclarator elem, Void arg) {
            return elem.getName();
        }

        @Override
        public String visitInterfaceRefDeclarator(InterfaceRefDeclarator elem, Void arg) {
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

}
