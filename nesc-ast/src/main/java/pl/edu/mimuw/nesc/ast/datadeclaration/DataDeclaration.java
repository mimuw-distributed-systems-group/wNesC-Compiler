package pl.edu.mimuw.nesc.ast.datadeclaration;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.NescDeclaration;
import pl.edu.mimuw.nesc.ast.Type;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;

/**
 * TODO
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public abstract class DataDeclaration {

	/**
	 * Data name.
	 */
	protected final String name;
	protected Type type;
	protected LinkedList<NescAttribute> attributes;
	/**
	 * The interface/module/configuration this declaration belongs to.
	 * <code>null</code> for declarations from C files.
	 */
	protected NescDeclaration container;

	public DataDeclaration(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public LinkedList<NescAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(LinkedList<NescAttribute> attributes) {
		this.attributes = attributes;
	}

	public NescDeclaration getContainer() {
		return container;
	}

	public void setContainer(NescDeclaration container) {
		this.container = container;
	}

	public interface Visitor<R, A> {
		R visit(ComponentRefDataDeclaration declaration, A arg);

		R visit(ConstDataDeclaration declaration, A arg);

		R visit(FuncDataDeclaration declaration, A arg);

		R visit(InterfaceRefDataDeclaration declaration, A arg);

		R visit(MagicStringDataDeclaration declaration, A arg);

		R visit(NetworkDataDeclaration declaration, A arg);

		R visit(TypevarDataDeclaration declaration, A arg);

		R visit(VarDataDeclaration declaration, A arg);

	}

	public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

}
