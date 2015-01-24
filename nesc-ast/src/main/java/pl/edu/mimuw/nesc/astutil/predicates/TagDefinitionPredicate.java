package pl.edu.mimuw.nesc.astutil.predicates;

import com.google.common.base.Predicate;
import java.util.List;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>The predicate is fulfilled if and only if the list of type elements
 * contains a tag definition, i.e. a definition of an enumerated type,
 * a structure, a union, an external structure or an external union. It does not
 * accept <code>null</code> values.</p>
 * <p>The predicate follows the singleton design pattern.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class TagDefinitionPredicate implements Predicate<List<? extends TypeElement>> {
    /**
     * The only instance of the predicate.
     */
    public static final TagDefinitionPredicate PREDICATE = new TagDefinitionPredicate();

    /**
     * Private constructor to limit its accessibility.
     */
    private TagDefinitionPredicate() {
    }

    @Override
    public boolean apply(List<? extends TypeElement> typeElements) {
        checkNotNull(typeElements, "tag reference cannot be null");

        for (TypeElement typeElement : typeElements) {
            if (!(typeElement instanceof TagRef)) {
                continue;
            }

            if (((TagRef) typeElement).getSemantics() != StructSemantics.OTHER) {
                return true;
            }
        }

        return false;
    }
}
