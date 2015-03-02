package pl.edu.mimuw.nesc.atomic;

import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.ComponentTyperef;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.EnumRef;
import pl.edu.mimuw.nesc.ast.gen.Enumerator;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.NxStructRef;
import pl.edu.mimuw.nesc.ast.gen.NxUnionRef;
import pl.edu.mimuw.nesc.ast.gen.Qualifier;
import pl.edu.mimuw.nesc.ast.gen.Rid;
import pl.edu.mimuw.nesc.ast.gen.StructRef;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.ast.gen.Typename;
import pl.edu.mimuw.nesc.ast.gen.TypeofExpr;
import pl.edu.mimuw.nesc.ast.gen.TypeofType;
import pl.edu.mimuw.nesc.ast.gen.UnionRef;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Visitor that modifies visited type elements and declarators for usage
 * as the type for the variable that holds the returned expression before
 * ending an atomic block.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class AtomicReturnTypeVisitor extends ExceptionVisitor<Void, Void> {
    private final NameMangler nameMangler;

    AtomicReturnTypeVisitor(NameMangler nameMangler) {
        checkNotNull(nameMangler, "name mangler cannot be null");
        this.nameMangler = nameMangler;
    }

    @Override
    public Void visitTypeofType(TypeofType typeElement, Void arg) {
        return null;
    }

    @Override
    public Void visitTypeofExpr(TypeofExpr typeElement, Void arg) {
        return null;
    }

    @Override
    public Void visitTypename(Typename typeElement, Void arg) {
        return null;
    }

    @Override
    public Void visitComponentTyperef(ComponentTyperef typeElement, Void arg) {
        return null;
    }

    @Override
    public Void visitStructRef(StructRef typeElement, Void arg) {
        modifyFieldTagRef(typeElement);
        return null;
    }

    @Override
    public Void visitNxStructRef(NxStructRef typeElement, Void arg) {
        modifyFieldTagRef(typeElement);
        return null;
    }

    @Override
    public Void visitUnionRef(UnionRef typeElement, Void arg) {
        modifyFieldTagRef(typeElement);
        return null;
    }

    @Override
    public Void visitNxUnionRef(NxUnionRef typeElement, Void arg) {
        modifyFieldTagRef(typeElement);
        return null;
    }

    @Override
    public Void visitEnumRef(EnumRef typeElement, Void arg) {
        for (Declaration enumerator : typeElement.getFields()) {
            enumerator.accept(this, null);
        }

        return null;
    }

    @Override
    public Void visitRid(Rid typeElement, Void arg) {
        return null;
    }

    @Override
    public Void visitQualifier(Qualifier typeElement, Void arg) {
        return null;
    }

    @Override
    public Void visitEnumerator(Enumerator enumerator, Void arg) {
        enumerator.setUniqueName(nameMangler.remangle(enumerator.getUniqueName()));
        return null;
    }

    private void modifyFieldTagRef(TagRef fieldTagRef) {
        // Transform the tag reference not to be the definition
        fieldTagRef.setFields(Lists.<Declaration>newList());
        fieldTagRef.setSemantics(StructSemantics.OTHER);
    }
}
