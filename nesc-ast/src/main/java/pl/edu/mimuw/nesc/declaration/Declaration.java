package pl.edu.mimuw.nesc.declaration;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.environment.Environment;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class Declaration {

    protected Location location;
    protected Environment environment;

    protected Declaration(Builder<? extends Declaration> builder) {
        this.location = builder.startLocation;
    }

    protected Declaration(Location startLocation) {
        /* FIXME introduce the builder pattern to all declaration classes and
           remove this constructor */
        this.location = startLocation;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Declaration other = (Declaration) obj;
        return Objects.equal(this.location, other.location);
    }

    /**
     * Base builder for an object that represents a declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static abstract class Builder<T extends Declaration> {
        /**
         * Data needed to construct the declaration object.
         */
        private Location startLocation;

        protected Builder() {
        }

        /**
         * Set the start location of the declaration.
         *
         * @param startLocation Start location of the declaration.
         * @return <code>this</code>
         */
        public Builder<T> startLocation(Location startLocation) {
            this.startLocation = startLocation;
            return this;
        }

        /**
         * Performs the building procedure. If the object to create has been
         * incorrectly specified, this method can throw an exception.
         *
         * @return The built instance.
         */
        public final T build() {
            beforeBuild();
            validate();
            return create();
        }

        /**
         * This method is called during the building process before validation
         * and creation of the object. It can be used to make some additional
         * operations before the actual build.
         */
        protected void beforeBuild() {
        }

        /**
         * @return Instance of the object that this builder builds.
         */
        protected abstract T create();

        /**
         * Performs a check if all data is properly set before creating the
         * object.
         */
        protected void validate() {
            checkNotNull(startLocation, "the start location cannot be null");
        }
    }
}
