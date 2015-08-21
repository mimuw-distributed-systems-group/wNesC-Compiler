package pl.edu.mimuw.nesc.astutil;

import java.util.Comparator;
import pl.edu.mimuw.nesc.ast.gen.NescDecl;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A total ordering imposed on NesC components and interfaces that is the
 * ascending order of their names.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class NescDeclComparator implements Comparator<NescDecl> {
    @Override
    public int compare(NescDecl nescDecl1, NescDecl nescDecl2) {
        checkNotNull(nescDecl1, "the first NesC declaration cannot be null");
        checkNotNull(nescDecl2, "the second NesC declaration cannot be null");
        return nescDecl1.getName().getName().compareTo(nescDecl2.getName().getName());
    }
}
