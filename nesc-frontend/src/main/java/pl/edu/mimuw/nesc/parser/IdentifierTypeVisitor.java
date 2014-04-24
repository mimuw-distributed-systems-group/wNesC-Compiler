package pl.edu.mimuw.nesc.parser;

import pl.edu.mimuw.nesc.declaration.object.*;

import static pl.edu.mimuw.nesc.parser.Parser.Lexer.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
class IdentifierTypeVisitor implements ObjectDeclaration.Visitor<Integer, Void> {
    @Override
    public Integer visit(ComponentRefDeclaration componentRef, Void arg) {
        return COMPONENTREF;
    }

    @Override
    public Integer visit(ConstantDeclaration constant, Void arg) {
        return IDENTIFIER;
    }

    @Override
    public Integer visit(FunctionDeclaration function, Void arg) {
        return IDENTIFIER;
    }

    @Override
    public Integer visit(InterfaceRefDeclaration interfaceRef, Void arg) {
        return IDENTIFIER;
    }

    @Override
    public Integer visit(TypenameDeclaration typename, Void arg) {
        return TYPEDEF_NAME;
    }

    @Override
    public Integer visit(VariableDeclaration variable, Void arg) {
        return IDENTIFIER;
    }
}
