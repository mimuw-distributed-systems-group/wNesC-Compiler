package pl.edu.mimuw.nesc.astbuilding.nesc;

import pl.edu.mimuw.nesc.ast.gen.AstType;
import pl.edu.mimuw.nesc.ast.gen.TypeArgument;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescExpressions {

    public static TypeArgument makeTypeArgument(AstType astType) {
        final TypeArgument typeArgument = new TypeArgument(astType.getLocation(), astType);
        typeArgument.setEndLocation(astType.getEndLocation());
        typeArgument.setType(astType.getType());
        return typeArgument;
    }

    private NescExpressions() {
    }

}
