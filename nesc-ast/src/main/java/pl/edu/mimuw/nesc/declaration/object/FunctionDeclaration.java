package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class FunctionDeclaration extends ObjectDeclaration {

    public static enum FunctionType {
        IMPLICIT, NORMAL, STATIC, NESTED, COMMAND, EVENT,
        /**
         * FIXME: in nesc compiler function type does not contain task value.
         * But it is convenient to use in proposal completion.
         */
        TASK,
    }

    private final Optional<String> ifaceName;

    private FunctionDeclarator astFunctionDeclarator;
    private FunctionType functionType;

    /**
     * Indicates whether function definition (do not be confused with
     * declaration!) have been already parsed.
     */
    private boolean isDefined;

    public FunctionDeclaration(String functionName, Location location) {
        this(functionName, location, null);
    }

    public FunctionDeclaration(String name, Location location, String ifaceName) {
        super(name, location);
        this.ifaceName = Optional.fromNullable(ifaceName);
        this.isDefined = false;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public String getName() {
        if (ifaceName.isPresent()) {
            return String.format("%s.%s", ifaceName.get(), name);
        }
        return name;
    }

    public Optional<String> getIfaceName() {
        return ifaceName;
    }

    public String getFunctionName() {
        return name;
    }

    public FunctionDeclarator getAstFunctionDeclarator() {
        return astFunctionDeclarator;
    }

    public void setAstFunctionDeclarator(FunctionDeclarator functionDeclarator) {
        this.astFunctionDeclarator = functionDeclarator;
    }

    public FunctionType getFunctionType() {
        return functionType;
    }

    public void setFunctionType(FunctionType functionType) {
        this.functionType = functionType;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public void setDefined(boolean isDefined) {
        this.isDefined = isDefined;
    }
}
