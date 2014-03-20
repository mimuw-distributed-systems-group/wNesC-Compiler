package pl.edu.mimuw.nesc.semantic.nesc;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.IdentifierDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Word;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class NescModule {

    public static InterfaceRefDeclarator makeInterfaceRefDeclarator(Location ifaceStartLocation, String ifaceName,
                                                                    Location funcNameStartLocation,
                                                                    Location funcNameEndLocation, String functionName) {
        final IdentifierDeclarator id = new IdentifierDeclarator(funcNameStartLocation, functionName);
        id.setEndLocation(funcNameEndLocation);
        final InterfaceRefDeclarator declarator = new InterfaceRefDeclarator(ifaceStartLocation, id,
                makeWord(ifaceStartLocation, funcNameEndLocation, ifaceName));
        return declarator;
    }

    public static Word makeWord(Location startLocation, Location endLocation, String name) {
        final Word word = new Word(startLocation, name);
        word.setEndLocation(endLocation);
        return word;
    }

    private NescModule() {
    }

}
