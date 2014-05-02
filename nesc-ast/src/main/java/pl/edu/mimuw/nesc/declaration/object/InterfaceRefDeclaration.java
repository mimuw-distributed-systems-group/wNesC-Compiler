package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRef;
import pl.edu.mimuw.nesc.declaration.nesc.InterfaceDeclaration;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class InterfaceRefDeclaration extends ObjectDeclaration {

    private final String ifaceName;

    private InterfaceRef astInterfaceRef;
    /**
     * Interface declaration (absent if reference is erroneous).
     */
    private Optional<InterfaceDeclaration> ifaceDeclaration;

    private boolean provides;

    public InterfaceRefDeclaration(String name, String ifaceName, Location location) {
        super(name, location);
        this.ifaceName = ifaceName;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    public String getIfaceName() {
        return ifaceName;
    }

    public InterfaceRef getAstInterfaceRef() {
        return astInterfaceRef;
    }

    public void setAstInterfaceRef(InterfaceRef astInterfaceRef) {
        this.astInterfaceRef = astInterfaceRef;
    }

    public Optional<InterfaceDeclaration> getIfaceDeclaration() {
        return ifaceDeclaration;
    }

    public void setIfaceDeclaration(Optional<InterfaceDeclaration> ifaceDeclaration) {
        this.ifaceDeclaration = ifaceDeclaration;
    }

    public boolean isProvides() {
        return provides;
    }

    public void setProvides(boolean provides) {
        this.provides = provides;
    }
}
