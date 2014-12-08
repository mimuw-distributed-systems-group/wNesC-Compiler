package pl.edu.mimuw.nesc.environment;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.symboltable.DefaultSymbolTable;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Default implementation of an environment.</p>
 * <p>Should be used for non-global scopes.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class DefaultEnvironment implements Environment {

    protected final Optional<Environment> parent;
    protected final SymbolTable<ObjectDeclaration> objects;
    protected final SymbolTable<TagDeclaration> tags;
    protected final List<Environment> enclosedEnvironments;

    protected boolean isNestedInGenericEntity;
    protected ScopeType type;
    protected Optional<Location> startLocation;
    protected Optional<Location> endLocation;

    public DefaultEnvironment() {
        this(null, Optional.<Environment>absent(), Optional.<Location>absent(), Optional.<Location>absent());
    }

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
        this.enclosedEnvironments = new ArrayList<>();
        this.isNestedInGenericEntity = parent.isPresent()
                && parent.get().isEnclosedInGenericNescEntity();
        if (parent.isPresent()) {
            parent.get().addEnclosedEnvironment(this);
        }
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
    public void setEndLocation(Location location) {
        this.endLocation = Optional.fromNullable(location);
    }

    @Override
    public ScopeType getScopeType() {
        return type;
    }

    @Override
    public void setScopeType(ScopeType type) {
        this.type = type;
    }

    @Override
    public void addEnclosedEnvironment(Environment environment) {
        this.enclosedEnvironments.add(environment);
    }

    @Override
    public List<Environment> getEnclosedEnvironments() {
        return this.enclosedEnvironments;
    }

    @Override
    public boolean isEnclosedInNescEntity() {
        switch (getScopeType()) {
            case INTERFACE_PARAMETER:
            case INTERFACE:
            case COMPONENT_PARAMETER:
            case SPECIFICATION:
            case MODULE_IMPLEMENTATION:
            case CONFIGURATION_IMPLEMENTATION:
                return true;
            default:
                return parent.isPresent() && parent.get().isEnclosedInNescEntity();
        }
    }

    @Override
    public boolean isEnclosedInGenericNescEntity() {
        return isNestedInGenericEntity;
    }

    @Override
    public void setEnclosedInGenericNescEntity(boolean value) {
        this.isNestedInGenericEntity = value;
    }

    @Override
    public boolean isObjectDeclaredInsideNescEntity(String name) {
        checkNotNull(name, "name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");

        return objects.contains(name, true)
                ? isEnclosedInNescEntity()
                : parent.isPresent() && parent.get().isObjectDeclaredInsideNescEntity(name);
    }

    @Override
    public boolean isTagDeclaredInsideNescEntity(String name) {
        checkNotNull(name, "name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");

        return tags.contains(name, true)
                ? isEnclosedInNescEntity()
                : parent.isPresent() && parent.get().isTagDeclaredInsideNescEntity(name);
    }
}
