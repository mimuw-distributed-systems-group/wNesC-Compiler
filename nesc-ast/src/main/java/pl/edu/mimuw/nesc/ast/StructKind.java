package pl.edu.mimuw.nesc.ast;

public enum StructKind {
    STRUCT(false),
    UNION(false),
    NX_STRUCT(true),
    NX_UNION(true),
    ENUM(false),
    ATTRIBUTE(false);

    private final boolean isExternal;

    private StructKind(boolean isExternal) {
        this.isExternal = isExternal;
    }

    /**
     * External tags are: external structures and external unions.
     *
     * @return <code>true</code> if and only if this kind is the kind of an
     *         external tag.
     */
    public boolean isExternal() {
        return isExternal;
    }
}
