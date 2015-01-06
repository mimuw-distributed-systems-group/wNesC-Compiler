package pl.edu.mimuw.nesc.ast;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public enum RID {
	INT("int"),
	CHAR("char"),
	FLOAT("float"),
	DOUBLE("double"),
	VOID("void"),

	UNSIGNED("unsigned"),
	SHORT("short"),
	LONG("long"),
	SIGNED("signed"),
	COMPLEX("_Complex"),

	INLINE("inline"),
	DEFAULT("default"),
	NORACE("norace"),

	AUTO("auto"),
	STATIC("static"),
	EXTERN("extern"),
	REGISTER("register"),
	TYPEDEF("typedef"),
	COMMAND("command"),
	EVENT("event"),
	TASK("task"),
	ASYNC("async"),

	CONST("const"),
	VOLATILE("volatile"),
	RESTRICT("restrict");

	private final String name;

	private RID(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}