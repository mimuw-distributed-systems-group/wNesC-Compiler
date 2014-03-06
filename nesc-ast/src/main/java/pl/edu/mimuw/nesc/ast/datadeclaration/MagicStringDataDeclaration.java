package pl.edu.mimuw.nesc.ast.datadeclaration;


public class MagicStringDataDeclaration extends DataDeclaration {

	public MagicStringDataDeclaration(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public <R, A> R accept(Visitor<R, A> visitor, A arg) {
		return visitor.visit(this, arg);
	}
}
