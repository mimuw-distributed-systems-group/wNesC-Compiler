package pl.edu.mimuw.nesc.preprocessor.directive;

/**
 * Default preprocessor directives visitor with empty actions.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class DefaultDirectiveVisitor<R, A> implements PreprocessorDirective.Visitor<R, A> {

    @Override
    public R visit(IncludeDirective include, A arg) {
        return null;
    }

    @Override
    public R visit(DefineDirective define, A arg) {
        return null;
    }

    @Override
    public R visit(UndefDirective undef, A arg) {
        return null;
    }

    @Override
    public R visit(IfDirective _if, A arg) {
        return null;
    }

    @Override
    public R visit(IfdefDirective ifdef, A arg) {
        return null;
    }

    @Override
    public R visit(IfndefDirective ifndef, A arg) {
        return null;
    }

    @Override
    public R visit(ElseDirective _else, A arg) {
        return null;
    }

    @Override
    public R visit(ElifDirective elif, A arg) {
        return null;
    }

    @Override
    public R visit(EndifDirective endif, A arg) {
        return null;
    }

    @Override
    public R visit(ErrorDirective error, A arg) {
        return null;
    }

    @Override
    public R visit(WarningDirective warning, A arg) {
        return null;
    }

    @Override
    public R visit(PragmaDirective pragma, A arg) {
        return null;
    }

    @Override
    public R visit(LineDirective line, A arg) {
        return null;
    }

    @Override
    public R visit(UnknownDirective erroneous, A arg) {
        return null;
    }
}
