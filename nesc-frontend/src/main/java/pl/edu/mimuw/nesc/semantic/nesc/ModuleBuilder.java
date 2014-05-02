package pl.edu.mimuw.nesc.semantic.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.ast.gen.Module;
import pl.edu.mimuw.nesc.ast.gen.Word;
import pl.edu.mimuw.nesc.declaration.nesc.ModuleDeclaration;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.issue.NescIssue;

import static java.lang.String.format;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
final class ModuleBuilder extends SemanticBase {

    private final Module module;
    private final ModuleDeclaration moduleDeclaration;

    public ModuleBuilder(NescEntityEnvironment nescEnvironment,
                         ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                         Module module) {
        super(nescEnvironment, issuesMultimapBuilder);
        this.module = module;
        this.moduleDeclaration = module.getDeclaration();
    }

    public void build() {
        final Word name = module.getName();

        if (!nescEnvironment.add(name.getName(), moduleDeclaration)) {
            errorHelper.error(name.getLocation(), Optional.of(name.getEndLocation()),
                    format("redefinition of '%s'", name.getName()));
        }

        /* Check parameters. */
        // TODO

        /* Check specification. */
        SpecificationBuilder specBuilder = new SpecificationBuilder(nescEnvironment, issuesMultimapBuilder, module);
        specBuilder.build();

        /* Check body. */
        // TODO
    }

}
