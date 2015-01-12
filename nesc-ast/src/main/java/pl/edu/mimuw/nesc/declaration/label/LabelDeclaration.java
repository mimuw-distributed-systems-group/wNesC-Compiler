package pl.edu.mimuw.nesc.declaration.label;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.Declaration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class LabelDeclaration extends Declaration {

    private final String name;

    /**
     * Value indicating if this label is a local label:
     *
     * https://gcc.gnu.org/onlinedocs/gcc/Local-Labels.html
     *
     */
    private final boolean isLocal;

    /**
     * <code>true</code> if and only if this label is defined.
     */
    private boolean isDefined;

    public static Builder builder() {
        return new Builder();
    }

    protected LabelDeclaration(Builder builder) {
        super(builder);
        this.name = builder.name;
        this.isLocal = builder.isLocal;
        this.isDefined = builder.isDefined;
    }

    public String getName() {
        return name;
    }

    /**
     * Check if this object is associated with a local label (GCC extension):
     *
     * https://gcc.gnu.org/onlinedocs/gcc/Local-Labels.html
     *
     * @return <code>true</code> if and only if this object is associated with
     *         a local label.
     */
    public boolean isLocal() {
        return isLocal;
    }

    /**
     * Check if this object is associated with a defined label. A definition
     * of a label is its name ended with a semicolon. Example of a declaration
     * of a label that is not simultaneously its definition:
     * <pre>
     *     {
     *         __label__ failure;  <---------- declaration but not definition
     *         &hellip;
     *         failure:            <---------- declaration and definition
     *            return 2;
     *     }
     * </pre>
     *
     * @return <code>true</code> if and only if this object is associated with
     *         a defined label.
     */
    public boolean isDefined() {
        return isDefined;
    }

    /**
     * Set this label as defined.
     */
    public void defined() {
        this.isDefined = true;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final LabelDeclaration other = (LabelDeclaration) obj;
        return Objects.equal(this.name, other.name);
    }

    /**
     * Builder for a label declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder extends Declaration.Builder<LabelDeclaration> {
        private String name;
        private boolean isLocal = false;
        private boolean isDefined = true;

        protected Builder() {
        }

        /**
         * Set the name of the label.
         *
         * @param name Name of the label to set.
         * @return <code>this</code>
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set fields from this builder to indicate that this label is
         * local and not defined.
         *
         * @return <code>this</code>
         */
        public Builder local() {
            this.isLocal = true;
            this.isDefined = false;
            return this;
        }

        /**
         * Set fields of this builder to indicate that this label is a normal
         * label and it is defined.
         *
         * @return <code>this</code>
         */
        public Builder nonlocal() {
            this.isLocal = false;
            this.isDefined = true;
            return this;
        }

        @Override
        protected void validate() {
            super.validate();
            checkNotNull(name, "name cannot be null");
        }

        @Override
        protected LabelDeclaration create() {
            return new LabelDeclaration(this);
        }
    }
}
