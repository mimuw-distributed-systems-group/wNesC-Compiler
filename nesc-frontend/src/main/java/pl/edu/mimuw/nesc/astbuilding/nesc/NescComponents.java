package pl.edu.mimuw.nesc.astbuilding.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astbuilding.AstBuildingBase;
import pl.edu.mimuw.nesc.declaration.nesc.InterfaceDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.issue.NescIssue;
import pl.edu.mimuw.nesc.token.Token;

import java.util.LinkedList;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.ast.AstUtils.makeWord;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescComponents extends AstBuildingBase {


    public NescComponents(ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                          ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder) {
        super(issuesMultimapBuilder, tokensMultimapBuilder);
    }

    public void declareInterfaceRef(Environment environment, InterfaceRef ifaceRef,
                                    Optional<LinkedList<Declaration>> genericParameters,
                                    LinkedList<Attribute> attributes) {
        ifaceRef.setGenericParameters(genericParameters);
        ifaceRef.setAttributes(attributes);

        final String ifaceName = ifaceRef.getName().getName();
        final String refName;
        final Location refLocation;
        final Location refEndLocation;
        if (ifaceRef.getAlias().isPresent()) {
            refName = ifaceRef.getAlias().get().getName();
            refLocation = ifaceRef.getAlias().get().getLocation();
            refEndLocation = ifaceRef.getAlias().get().getEndLocation();
        } else {
            refName = ifaceRef.getName().getName();
            refLocation = ifaceRef.getName().getLocation();
            refEndLocation = ifaceRef.getName().getEndLocation();
        }

        final InterfaceRefDeclaration declaration = new InterfaceRefDeclaration(refName, ifaceName, refLocation);
        declaration.setAstInterfaceRef(ifaceRef);
        if (!environment.getObjects().add(refName, declaration)) {
            errorHelper.error(refLocation, Optional.of(refEndLocation), format("redefinition of '%s'", refName));
        }
        ifaceRef.setDeclaration(declaration);
    }

    public InterfaceRefDeclarator makeInterfaceRefDeclarator(Location ifaceStartLocation, String ifaceName,
                                                             Location funcNameStartLocation,
                                                             Location funcNameEndLocation, String functionName) {
        final IdentifierDeclarator id = new IdentifierDeclarator(funcNameStartLocation, functionName);
        id.setEndLocation(funcNameEndLocation);
        final InterfaceRefDeclarator declarator = new InterfaceRefDeclarator(ifaceStartLocation, id,
                makeWord(ifaceStartLocation, funcNameEndLocation, ifaceName));
        return declarator;
    }

}
