package pl.edu.mimuw.nesc.ast.datadeclaration;

/**
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class VarDataDeclaration extends DataDeclaration {

	/**
	 * Variable type.
	 * 
	 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
	 * 
	 */
	public static enum VariableType {
		REGISTER, STATIC, NORMAL;
	};

	public VarDataDeclaration(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public <R, A> R accept(Visitor<R, A> visitor, A arg) {
		return visitor.visit(this, arg);
	}
}
