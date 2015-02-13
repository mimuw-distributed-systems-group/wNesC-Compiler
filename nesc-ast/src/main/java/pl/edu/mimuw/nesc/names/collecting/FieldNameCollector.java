package pl.edu.mimuw.nesc.names.collecting;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import java.util.Collection;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.EnumRef;
import pl.edu.mimuw.nesc.ast.gen.FieldDecl;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;

/**
 * Collector of fields names of tags. Names of fields from anonymous structures
 * and unions are also collected. References to enumerations are ignored.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class FieldNameCollector extends AbstractNameCollector<TagRef> {
    @Override
    public void collect(TagRef tagReference) {
        if (tagReference.getSemantics() != StructSemantics.DEFINITION
                || tagReference instanceof EnumRef) {
            return;
        }

        for (Declaration outerDecl : tagReference.getFields()) {
            if (!(outerDecl instanceof DataDecl)) {
                continue;
            }

            final DataDecl dataDecl = (DataDecl) outerDecl;

            if (!dataDecl.getDeclarations().isEmpty()) {
                for (Declaration innerDecl : dataDecl.getDeclarations()) {
                    if (!(innerDecl instanceof FieldDecl)) {
                        continue;
                    }

                    final FieldDecl fieldDecl = (FieldDecl) innerDecl;

                    if (fieldDecl.getDeclarator().isPresent()) {
                        final Optional<String> name = DeclaratorUtils.getDeclaratorName(fieldDecl.getDeclarator().get());
                        if (name.isPresent()) {
                            names.add(name.get());
                        }
                    }
                }
            } else {
                for (TypeElement typeElement : dataDecl.getModifiers()) {
                    if (typeElement instanceof TagRef) {
                        collect((TagRef) typeElement);
                    }
                }
            }
        }
    }

    @Override
    public void collect(Collection<?> objects) {
        checkCollection(objects);

        final FluentIterable<TagRef> tagReferences = FluentIterable.from(objects)
                .filter(TagRef.class);

        for (TagRef tagReference : tagReferences) {
            collect(tagReference);
        }
    }
}
