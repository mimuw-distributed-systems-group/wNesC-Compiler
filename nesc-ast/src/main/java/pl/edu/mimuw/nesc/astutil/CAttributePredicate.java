package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import java.util.List;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Predicate on a list of attributes that is <code>true</code> if and only
 * if the list of attributes contains the @C() NesC attribute.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class CAttributePredicate implements Predicate<List<? extends Attribute>> {
    /**
     * Start location of the @C() attribute.
     */
    private Optional<Location> startLocation;

    /**
     * End location of the detected @C() attribute.
     */
    private Optional<Location> endLocation;

    @Override
    public boolean apply(List<? extends Attribute> attributes) {
        checkNotNull(attributes, "the attributes list cannot be null");

        for (Attribute attribute : attributes) {
            if (!(attribute instanceof NescAttribute)) {
                continue;
            }

            final NescAttribute nescAttribute = (NescAttribute) attribute;
            if ("C".equals(nescAttribute.getName().getName())) {
                this.startLocation = Optional.of(nescAttribute.getLocation());
                this.endLocation = Optional.of(nescAttribute.getEndLocation());
                return true;
            }
        }

        this.startLocation = this.endLocation = Optional.absent();
        return false;
    }

    /**
     * Get the start location of the detected @C() attribute.
     *
     * @return Start location of the detected @C() attribute.
     */
    public Optional<Location> getStartLocation() {
        return startLocation;
    }

    /**
     * Get the end location of the detected @C() attribute.
     *
     * @return End location of the @C() attribute.
     */
    public Optional<Location> getEndLocation() {
        return endLocation;
    }
}
