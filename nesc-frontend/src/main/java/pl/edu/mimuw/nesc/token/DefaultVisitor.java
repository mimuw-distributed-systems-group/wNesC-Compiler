package pl.edu.mimuw.nesc.token;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class DefaultVisitor<R, A> implements Token.Visitor<R, A> {

    @Override
    public R visit(CharacterToken character, A arg) {
        return null;
    }

    @Override
    public R visit(NumberToken number, A arg) {
        return null;
    }

    @Override
    public R visit(StringToken string, A arg) {
        return null;
    }

    @Override
    public R visit(KeywordToken keyword, A arg) {
        return null;
    }

    @Override
    public R visit(PunctuationToken punctuation, A arg) {
        return null;
    }

    @Override
    public R visit(IdToken id, A arg) {
        return null;
    }
}
