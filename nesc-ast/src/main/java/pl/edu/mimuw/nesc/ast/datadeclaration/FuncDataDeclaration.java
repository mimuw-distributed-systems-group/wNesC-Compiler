package pl.edu.mimuw.nesc.ast.datadeclaration;

/**
 * TODO
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class FuncDataDeclaration extends DataDeclaration {

	/**
	 * Function type.
	 * 
	 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
	 * 
	 */
	public static enum FunctionType {
		IMPLICIT, NORMAL, STATIC, NESTED, EVENT, COMMAND;
	};
	
	protected FunctionType functionType;

	public FuncDataDeclaration(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public <R, A> R accept(Visitor<R, A> visitor, A arg) {
		return visitor.visit(this, arg);
	}
}
