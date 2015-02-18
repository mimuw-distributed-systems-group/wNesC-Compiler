package pl.edu.mimuw.nesc.token;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;

/**
 * Objects of this class represent a macro that has been used in the source code.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class MacroToken extends Token {
    /**
     * Location of the first letter of the macro name in its definition. It
     * can be absent, e.g. in the case of predefined macros like
     * <code>__LINE__</code>.
     */
    private final Optional<Location> definitionStartLocation;

    /**
     * Name of the macro that has been used.
     */
    private final String macroName;

    /**
     * Initializes the object with given arguments.
     */
    public MacroToken(Location startLocation, Location endLocation, String macroName,
                      Optional<Location> definitionStartLocation) {
        super(startLocation, endLocation);
        this.definitionStartLocation = definitionStartLocation;
        this.macroName = macroName;
    }

    /**
     * @return Location of the first letter of the macro name in its definition.
     *         It may not be always available, e.g. for predefined macros like
     *         <code>__LINE__</code>.
     */
    public Optional<Location> getDefinitionLocation() { return definitionStartLocation; }

    /**
     * @return Name of the macro that has been used.
     */
    public String getMacroName() { return macroName; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("startLocation", startLocation)
                .add("endLocation", endLocation)
                .add("macroName", macroName)
                .add("definitionLocation", definitionStartLocation)
                .toString();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(definitionStartLocation, macroName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final MacroToken macroToken = (MacroToken) o;
        return Objects.equal(definitionStartLocation, macroToken.definitionStartLocation)
                   && Objects.equal(macroName, macroToken.macroName);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) { return visitor.visit(this, arg); }
}
