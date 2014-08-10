package pl.edu.mimuw.nesc.environment;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;

import java.util.List;

/**
 * <p>Represents a global environment.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class TranslationUnitEnvironment extends DefaultEnvironment {

    @Override
    public Optional<Environment> getParent() {
        return Optional.absent();
    }

    @Override
    public Optional<Location> getStartLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStartLocation(Location location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Location> getEndLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEndLocation(Location location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScopeType getScopeType() {
        return ScopeType.GLOBAL;
    }

    @Override
    public void setScopeType(ScopeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addEnclosedEnvironment(Environment environment) {
        this.enclosedEnvironments.add(environment);
    }

    @Override
    public List<Environment> getEnclosedEnvironments() {
        return this.enclosedEnvironments;
    }
}
