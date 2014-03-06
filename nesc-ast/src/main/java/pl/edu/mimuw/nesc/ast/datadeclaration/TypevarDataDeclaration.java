package pl.edu.mimuw.nesc.ast.datadeclaration;

/**
 * TODO
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class TypevarDataDeclaration extends DataDeclaration {

	public static enum TypevarKind {
		NONE, NORMAL, INTEGER, NUMBER;
	}

	protected TypevarKind typevarKind;

	public TypevarDataDeclaration(String name) {
		super(name);
		// TODO AutoF-generated constructor stub
	}

	@Override
	public <R, A> R accept(Visitor<R, A> visitor, A arg) {
		return visitor.visit(this, arg);
	}
}
