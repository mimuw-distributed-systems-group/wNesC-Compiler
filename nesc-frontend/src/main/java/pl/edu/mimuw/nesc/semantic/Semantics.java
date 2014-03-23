package pl.edu.mimuw.nesc.semantic;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.common.util.list.Lists;

import java.util.LinkedList;

import static pl.edu.mimuw.nesc.ast.AstUtils.getEndLocation;
import static pl.edu.mimuw.nesc.ast.AstUtils.getStartLocation;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class Semantics {

    /**
     * <p>Finishes array of function declarator.</p>
     * <h3>Example</h3>
     * <p><code>Foo.bar(int baz)</code>
     * where <code>Foo.bar</code> is <code>nested</code>, <code>(int baz)</code>
     * is <code>declarator</code></p>
     *
     * @param nested     declarator that precedes array or function parentheses
     *                   (e.g. plain identifier or interface reference)
     * @param declarator declarator containing array indices declaration or
     *                   function parameters
     * @return declarator combining these two declarators
     */
    public static Declarator finishArrayOrFnDeclarator(Declarator nested, NestedDeclarator declarator) {
        declarator.setLocation(nested.getLocation());
        declarator.setDeclarator(nested);
        return declarator;
    }

    public static FunctionDecl startFunction(Location startLocation, LinkedList<TypeElement> modifiers,
                                             Declarator declarator, LinkedList<Attribute> attributes,
                                             boolean isNested) {
        return new FunctionDecl(startLocation, declarator, modifiers, attributes, null, isNested);
    }

    public static FunctionDecl setOldParams(FunctionDecl functionDecl, LinkedList<Declaration> oldParams) {
        functionDecl.setOldParms(oldParams);
        return functionDecl;
    }

    public static FunctionDecl finishFunction(FunctionDecl functionDecl, Statement body) {
        functionDecl.setBody(body);
        functionDecl.setEndLocation(body.getEndLocation());
        return functionDecl;
    }

    /**
     * <p>Create definition of function parameter
     * <code>elements declarator</code> with attributes.</p>
     * <p>There must be at least a <code>declarator</code> or some form of type
     * specification.</p>
     *
     * @param declarator parameter declarator
     * @param elements   type elements
     * @param attributes attributes list (maybe empty)
     * @return the declaration for the parameter
     */
    public static DataDecl declareParameter(Optional<Declarator> declarator, LinkedList<TypeElement> elements,
                                            LinkedList<Attribute> attributes) {
        /*
         * The order of entities:
         * elements [declarator] [attributes]
         */
        /* Create variable declarator. */
        final Location varStartLocation;
        final Location varEndLocation;
        if (declarator.isPresent()) {
            varStartLocation = declarator.get().getLocation();
            varEndLocation = getEndLocation(declarator.get().getEndLocation(), attributes);
        } else {
            varStartLocation = getStartLocation(elements).get();
            varEndLocation = getEndLocation(elements, attributes).get();
        }
        final VariableDecl variableDecl = new VariableDecl(varStartLocation, declarator.orNull(), attributes,
                null, null);
        variableDecl.setEndLocation(varEndLocation);

        /* Create parameter declarator. */
        final Location startLocation = getStartLocation(elements).get();
        final Location endLocation = declarator.isPresent()
                ? getEndLocation(declarator.get().getEndLocation(), attributes)
                : getEndLocation(elements, attributes).get();

        final DataDecl dataDecl = new DataDecl(startLocation, elements, Lists.<Declaration>newList(variableDecl));
        dataDecl.setEndLocation(endLocation);
        return dataDecl;
    }

    public static OldIdentifierDecl declareOldParameter(Location startLocation, Location endLocation, String id) {
        final OldIdentifierDecl decl = new OldIdentifierDecl(startLocation, id);
        decl.setEndLocation(endLocation);
        return decl;
    }

    public static TagRef makeStruct(Location startLocation, Location endLocation, StructKind kind, Optional<Word> tag,
                                    LinkedList<Declaration> fields, LinkedList<Attribute> attributes) {
        return makeTagRef(startLocation, endLocation, kind, tag, fields, attributes);
    }

    public static TagRef makeEnum(Location startLocation, Location endLocation, Optional<Word> tag,
                                  LinkedList<Declaration> fields, LinkedList<Attribute> attributes) {
        return makeTagRef(startLocation, endLocation, StructKind.ENUM, tag, fields, attributes);
    }

    /**
     * Returns a reference to struct, union or enum.
     *
     * @param startLocation start location
     * @param endLocation   end location
     * @param structKind    kind
     * @param tag           name
     * @return struct/union/enum reference
     */
    public static TagRef makeXrefTag(Location startLocation, Location endLocation, StructKind structKind, Word tag) {
        return makeTagRef(startLocation, endLocation, structKind, Optional.of(tag));
    }

    /**
     * Creates declaration of field
     * <code>elements declarator : bitfield</code> with attributes.
     * <code>declarator</code> and <code>bitfield</code> cannot be both
     * absent.
     *
     * @param startLocation start location
     * @param endLocation   end location
     * @param declarator    declarator
     * @param bitfield      bitfield
     * @param elements      elements
     * @param attributes    attributes
     * @return declaration of field
     */
    public static FieldDecl makeField(Location startLocation, Location endLocation,
                                      Optional<Declarator> declarator, Optional<Expression> bitfield,
                                      LinkedList<TypeElement> elements, LinkedList<Attribute> attributes) {
        // FIXME: elements?
        endLocation = getEndLocation(endLocation, attributes);
        final FieldDecl decl = new FieldDecl(startLocation, declarator.orNull(), attributes, bitfield.orNull());
        decl.setEndLocation(endLocation);
        return decl;
    }

    public static Enumerator makeEnumerator(Location startLocation, Location endLocation, String id,
                                            Optional<Expression> value) {

        final Enumerator enumerator = new Enumerator(startLocation, id, value.orNull());
        enumerator.setEndLocation(endLocation);
        return enumerator;
    }

    public static AstType makeType(LinkedList<TypeElement> elements, Optional<Declarator> declarator) {
        final Location startLocation;
        final Location endLocation;
        if (declarator.isPresent()) {
            startLocation = getStartLocation(declarator.get().getLocation(), elements);
            endLocation = declarator.get().getEndLocation();
        } else {
            startLocation = getStartLocation(elements).get();
            endLocation = getEndLocation(elements).get();
        }
        final AstType type = new AstType(startLocation, declarator.orNull(), elements);
        type.setEndLocation(endLocation);
        return type;
    }

    public static Declarator makePointerDeclarator(Location startLocation, Location endLocation,
                                                   Optional<Declarator> declarator,
                                                   LinkedList<TypeElement> qualifiers) {
        final Location qualifiedDeclStartLocation = getStartLocation(
                declarator.isPresent()
                        ? declarator.get().getLocation()
                        : startLocation,
                qualifiers);
        final QualifiedDeclarator qualifiedDeclarator = new QualifiedDeclarator(qualifiedDeclStartLocation,
                declarator.orNull(), qualifiers);
        qualifiedDeclarator.setEndLocation(declarator.isPresent()
                ? declarator.get().getEndLocation()
                : endLocation);

        final PointerDeclarator pointerDeclarator = new PointerDeclarator(startLocation, qualifiedDeclarator);
        pointerDeclarator.setEndLocation(endLocation);
        return pointerDeclarator;
    }

    public static Rid makeRid(Location startLocation, Location endLocation, RID rid) {
        final Rid result = new Rid(startLocation, rid);
        result.setEndLocation(endLocation);
        return result;
    }

    public static Qualifier makeQualifier(Location startLocation, Location endLocation, RID rid) {
        final Qualifier result = new Qualifier(startLocation, rid);
        result.setEndLocation(endLocation);
        return result;
    }

    private static TagRef makeTagRef(Location startLocation, Location endLocation, StructKind structKind,
                                     Optional<Word> tag) {
        final LinkedList<Attribute> attributes = Lists.newList();
        final LinkedList<Declaration> declarations = Lists.newList();
        return makeTagRef(startLocation, endLocation, structKind, tag, declarations, attributes);
    }

    private static TagRef makeTagRef(Location startLocation, Location endLocation, StructKind structKind,
                                     Optional<Word> tag, LinkedList<Declaration> declarations,
                                     LinkedList<Attribute> attributes) {
        final TagRef tagRef;
        switch (structKind) {
            case STRUCT:
                tagRef = new StructRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            case UNION:
                tagRef = new UnionRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            case NX_STRUCT:
                tagRef = new NxStructRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            case NX_UNION:
                tagRef = new NxUnionRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            case ENUM:
                tagRef = new EnumRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            case ATTRIBUTE:
                tagRef = new AttributeRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            default:
                throw new IllegalArgumentException("Unexpected argument " + structKind);
        }
        tagRef.setEndLocation(endLocation);
        return tagRef;
    }

    private Semantics() {
    }

}
