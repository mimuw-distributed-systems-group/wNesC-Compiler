package pl.edu.mimuw.nesc.declaration.label;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.Declaration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class LabelDeclaration extends Declaration {

    private final String name;

    public static Builder builder() {
        return new Builder();
    }

    protected LabelDeclaration(Builder builder) {
        super(builder);
        this.name = builder.name;
    }

    public String getName() {
        return name;
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

        @Override
        protected void validate() {
            super.validate();
            checkNotNull(name, "name cannot be null");
        }

        @Override
        public LabelDeclaration create() {
            return new LabelDeclaration(this);
        }
    }
}
