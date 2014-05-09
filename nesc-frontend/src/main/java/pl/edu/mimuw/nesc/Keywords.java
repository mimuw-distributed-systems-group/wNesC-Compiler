package pl.edu.mimuw.nesc;

import com.google.common.collect.ImmutableList;

/**
 * Keywords container.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class Keywords {

    public static final ImmutableList<String> KEYWORDS;
    public static final ImmutableList<String> C_KEYWORDS;
    public static final ImmutableList<String> NESC_KEYWORDS;
    public static final ImmutableList<String> EXTENSION_KEYWORDS;
    public static final ImmutableList<String> CORE_KEYWORDS;

    static {
        NESC_KEYWORDS = ImmutableList.<String>builder()
                .add("as")
                .add("abstract")
                .add("async")
                .add("atomic")
                .add("call")
                .add("command")
                .add("component")
                .add("components")
                .add("configuration")
                .add("event")
                .add("extends")
                .add("generic")
                .add("implementation")
                .add("interface")
                .add("module")
                .add("new")
                .add("norace")
                .add("post")
                .add("provides")
                .add("signal")
                .add("task")
                .add("uses")
                .add("nx_struct")
                .add("nx_union")
                .build();

        C_KEYWORDS = ImmutableList.<String>builder()
                .add("auto")
                .add("break")
                .add("case")
                .add("char")
                .add("const")
                .add("continue")
                .add("default")
                .add("do")
                .add("double")
                .add("else")
                .add("enum")
                .add("extern")
                .add("float")
                .add("for")
                .add("goto")
                .add("if")
                .add("inline")
                .add("int")
                .add("long")
                .add("register")
                .add("restrict")
                .add("return")
                .add("short")
                .add("signed")
                .add("sizeof")
                .add("static")
                .add("struct")
                .add("switch")
                .add("typedef")
                .add("union")
                .add("unsigned")
                .add("void")
                .add("volatile")
                .add("while")
                .build();

        EXTENSION_KEYWORDS = ImmutableList.<String>builder()
                .add("asm")
                .add("offsetof")
                .add("__alignof__")
                .add("__asm")
                .add("__asm__")
                .add("__attribute")
                .add("__attribute__")
                .add("__builtin_offsetof")
                .add("__builtin_va_arg")
                .add("__complex")
                .add("__complex__")
                .add("__const")
                .add("__const__")
                .add("__extension__")
                .add("__imag")
                .add("__imag__")
                .add("__inline")
                .add("__inline__")
                .add("__label__")
                .add("__real")
                .add("__real__")
                .add("__restrict")
                .add("__signed")
                .add("__signed__")
                .add("__typeof")
                .add("__typeof__")
                .add("__volatile")
                .add("__volatile__")
                .build();

        CORE_KEYWORDS = ImmutableList.<String>builder()
                .addAll(C_KEYWORDS)
                .addAll(NESC_KEYWORDS)
                .build();

        KEYWORDS = ImmutableList.<String>builder()
                .addAll(C_KEYWORDS)
                .addAll(NESC_KEYWORDS)
                .addAll(EXTENSION_KEYWORDS)
                .build();
    }

}
