package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;

/**
 * This class represents tags that can be external. The only such tags are
 * structures and unions.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class MaybeExternalTagDeclaration extends TagDeclaration {
    /**
     * <code>true</code> if and only if this declaration corresponds to an
     * external tag declaration.
     */
    private final boolean isExternal;

    /* TODO pointers to definitions of structures, unions and enumerations
       for type analysis */

    protected MaybeExternalTagDeclaration(Optional<String> maybeName, Location location,
                                          boolean isDefined, boolean isExternal) {
        super(maybeName, location, isDefined);
        this.isExternal = isExternal;
    }

    public final boolean isExternal() {
        return isExternal;
    }
}
