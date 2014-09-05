package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.ComponentRef;
import pl.edu.mimuw.nesc.ast.gen.Word;
import pl.edu.mimuw.nesc.ast.type.ComponentType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.nesc.NescDeclaration;

/**
 * <p>Component reference.</p>
 * <p><code>name</code> is not the name of component but the name of the
 * reference (component can be aliased).</p>
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ComponentRefDeclaration extends ObjectDeclaration {

    private final Word componentName;

    private ComponentRef astComponentRef;
    /**
     * Component declaration (absent if reference is erroneous).
     */
    private Optional<? extends NescDeclaration> componentDeclaration;

    public ComponentRefDeclaration(String name, Word componentName, Location location) {
        super(name, location, Optional.of((Type) new ComponentType(componentName.getName())));
        this.componentName = componentName;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    public Word getComponentName() {
        return componentName;
    }

    public ComponentRef getAstComponentRef() {
        return astComponentRef;
    }

    public void setAstComponentRef(ComponentRef astComponentRef) {
        this.astComponentRef = astComponentRef;
    }

    public Optional<? extends NescDeclaration> getComponentDeclaration() {
        return componentDeclaration;
    }

    public void setComponentDeclaration(Optional<? extends NescDeclaration> componentDeclaration) {
        this.componentDeclaration = componentDeclaration;
    }
}
