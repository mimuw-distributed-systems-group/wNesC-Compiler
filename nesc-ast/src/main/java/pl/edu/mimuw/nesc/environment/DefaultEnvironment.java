package pl.edu.mimuw.nesc.environment;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.label.LabelDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.symboltable.DefaultLabelSymbolTable;
import pl.edu.mimuw.nesc.symboltable.DefaultSymbolTable;
import pl.edu.mimuw.nesc.symboltable.LabelSymbolTable;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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
    protected final Optional<? extends LabelSymbolTable<LabelDeclaration>> labels;
    protected final List<Environment> enclosedEnvironments;

    protected Optional<String> nescEntityName;
    protected boolean isNestedInGenericEntity;
    protected ScopeType type;
    protected Optional<Location> startLocation;
    protected Optional<Location> endLocation;

    public DefaultEnvironment() {
        this(null, Optional.<Environment>absent(), Optional.<Location>absent(),
                Optional.<Location>absent(), false);
    }

    public DefaultEnvironment(Environment parent) {
        this(null, Optional.of(parent), Optional.<Location>absent(),
                Optional.<Location>absent(), false);
    }

    public DefaultEnvironment(Environment parent, boolean functionTopLevelEnvironment) {
        this(null, Optional.of(parent), Optional.<Location>absent(),
                Optional.<Location>absent(), true);
    }

    public DefaultEnvironment(ScopeType type,
                              Optional<Environment> parent,
                              Optional<Location> startLocation,
                              Optional<Location> endLocation,
                              boolean functionTopLevelEnvironment) {
        this.type = type;
        this.parent = parent;
        this.objects = new DefaultSymbolTable<>(parent.isPresent() ?
                Optional.of(parent.get().getObjects()) :
                Optional.<SymbolTable<ObjectDeclaration>>absent());
        this.tags = new DefaultSymbolTable<>(parent.isPresent() ?
                Optional.of(parent.get().getTags()) :
                Optional.<SymbolTable<TagDeclaration>>absent());

        if (functionTopLevelEnvironment) {
            this.labels = Optional.of(DefaultLabelSymbolTable.newFunctionTopLevelTable(parent.isPresent() ?
                parent.get().getLabels() :
                Optional.<LabelSymbolTable<LabelDeclaration>>absent()));
        } else {
            this.labels = parent.isPresent() && parent.get().getLabels().isPresent()
                    ? Optional.of(DefaultLabelSymbolTable.newBlockTable(parent.get().getLabels().get()))
                    : Optional.<LabelSymbolTable<LabelDeclaration>>absent();
        }

        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.enclosedEnvironments = new ArrayList<>();
        this.nescEntityName = parent.isPresent()
                ? parent.get().getNescEntityName()
                : Optional.<String>absent();
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
    public Optional<? extends LabelSymbolTable<LabelDeclaration>> getLabels() {
        return labels;
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
        return this.nescEntityName.isPresent();
    }

    @Override
    public Optional<String> getNescEntityName() {
        return this.nescEntityName;
    }

    @Override
    public void setNescEntityName(String name) {
        checkNotNull(name, "name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");
        checkState(!this.nescEntityName.isPresent(), "NesC entity name is already set");

        this.nescEntityName = Optional.of(name);
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

    @Override
    public boolean isEnclosedInScope(ScopeType scopeType) {
        checkNotNull(scopeType, "scope type cannot be null");
        return getScopeType() == scopeType
                || parent.isPresent() && parent.get().isEnclosedInScope(scopeType);
    }
}
