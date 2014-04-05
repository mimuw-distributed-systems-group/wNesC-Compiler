package pl.edu.mimuw.nesc.environment;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.symboltable.DefaultSymbolTable;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

/**
 * <p>Default implementation of environment.</p>
 * <p>Should be used for non-global scopes.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class DefaultEnvironment implements Environment {

    private final Optional<Environment> parent;
    private final SymbolTable<ObjectDeclaration> objects;
    private final SymbolTable<TagDeclaration> tags;

    private ScopeType type;
    private Optional<Location> startLocation;
    private Optional<Location> endLocation;


    public DefaultEnvironment(Environment parent) {
        this(null, Optional.of(parent), Optional.<Location>absent(), Optional.<Location>absent());
    }

    public DefaultEnvironment(ScopeType type,
                              Optional<Environment> parent,
                              Optional<Location> startLocation,
                              Optional<Location> endLocation) {
        this.type = type;
        this.parent = parent;
        this.objects = new DefaultSymbolTable<>(parent.isPresent() ?
                Optional.of(parent.get().getObjects()) :
                Optional.<SymbolTable<ObjectDeclaration>>absent());
        this.tags = new DefaultSymbolTable<>(parent.isPresent() ?
                Optional.of(parent.get().getTags()) :
                Optional.<SymbolTable<TagDeclaration>>absent());
        this.startLocation = startLocation;
        this.endLocation = endLocation;
    }

    @Override
    public Optional<Environment> getParent() {
        return parent;
    }

    @Override
    public SymbolTable<ObjectDeclaration> getObjects() {
        return objects;
    }

    @Override
    public SymbolTable<TagDeclaration> getTags() {
        return tags;
    }

    @Override
    public Optional<Location> getStartLocation() {
        return startLocation;
    }

    @Override
    public void setStartLocation(Location location) {
        this.startLocation = Optional.of(location);
    }

    @Override
    public Optional<Location> getEndLocation() {
        return endLocation;
    }

    @Override
    public void setEndLocation(Location endLocation) {
        this.endLocation = Optional.of(endLocation);
    }

    @Override
    public ScopeType getScopeType() {
        return type;
    }

    @Override
    public void setScopeType(ScopeType type) {
        this.type = type;
    }
}
