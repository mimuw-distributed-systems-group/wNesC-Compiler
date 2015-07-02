from os import path, makedirs
from collections import OrderedDict

from ast.util import first_to_cap, DST_LANGUAGE, tab, ast_nodes, ast_enums, \
    generic_nodes, mangle_nodes, unique_nodes, language_dispatch
from ast.field_copy import *
from ast.fields import BasicASTNodeField, BoolField, ReferenceField, \
    ReferenceListField, EnumField, EnumListField, StringField

#TODO:
#   - protection in printer from printing null
#   - proper printing of Optional parameters
#   - check if all fields can be optional
#   - fix c++ generation
#   - reformat the code
#   - divide the code into sensible modules
#TODO: add standard ast options:
#   - specify package option for java
#   - option to generate a jar file
#   - ability to say that a class is virtual?
#   - some objects represent operators and do not have fields
#     -> add an option of a class header (both a global and a local one)
#   - allow fro creating a context, so that many different packages of AST can be generated


#This metaclass is used for registering ast nodes in our dictionary
#to make the generation process easier
class ASTElemMetaclass(type):
    @classmethod
    def __prepare__(metacls, name, bases, *args, **kwargs):
        return OrderedDict()

    def __new__(cls, name, bases, namespace, *args, **kwargs):
        #newclass = super(cls, ASTElemMetaclass)
        #newclass = newclass.__new__(cls, clsname, bases, attrs)

        #Add the pasted field for nodes with activated generic indicator
        if name != 'BasicASTNode' and name != 'BasicASTEnum':
            if "mangleIndicator" in namespace:
                indicator = namespace["mangleIndicator"]
                if indicator.activated:
                    namespace[indicator.unique_field_name] = StringField(constructor_variable=False,
                                                                         optional=indicator.optional)
                    namespace[indicator.remangle_flag_name] = BoolField(constructor_variable=False)

        newclass = type.__new__(cls, name, bases, dict(namespace))
        newclass.members__ = tuple(namespace)

        if name != 'BasicASTNode' and name != 'BasicASTEnum':
            if "BasicASTNode" in map(lambda x: x.__name__, bases):
                ast_nodes[first_to_cap(name)] = newclass
            if "BasicASTEnum" in map(lambda x: x.__name__, bases):
                ast_enums[first_to_cap(name)] = newclass
            if "genericIndicator" in namespace:
                generic_nodes[name] = newclass
            if "mangleIndicator" in namespace:
                indicator = namespace["mangleIndicator"]
                if indicator.activated:
                    mangle_nodes[name] = (newclass, indicator)
            if "uniqueIndicator" in namespace:
                unique_nodes[name] = namespace["uniqueIndicator"]

        return newclass


#The basic class from which all ast node classes must derive
class BasicASTNode(metaclass=ASTElemMetaclass):
    #generates the constructor for the desired language
    def gen_constructor(self, lang):
        cls = self.__class__

        type_dict = dict(cls.__field_types)
        type_dict.update(cls.__super_field_types)

        default_dict = dict(cls.__field_defaults)
        default_dict.update(cls.__super_field_defaults)

        all_constructor_params = list(filter(lambda x: type_dict[x][2],
                                             cls.__super_field_names + cls.__field_names))

        fields_id_dict = dict(map(lambda x: (x[1], x[0]),
                                  enumerate(all_constructor_params)))
        non_const_fields = filter(lambda x: not type_dict[x][0], all_constructor_params)
        const_fields = filter(lambda x: type_dict[x][0], all_constructor_params)
        default_fields = filter(lambda x: x in default_dict, all_constructor_params)

        code = tab
        args = (",\n" + tab * 4).join(["{0} arg{1}".format(type_dict[f][1], i)
                                       for i, f in enumerate(all_constructor_params)])
        body = ""
        const_init = " "

        if lang == DST_LANGUAGE.CPP:
            body = "\n".join([2 * tab + "this->{0} = arg{1};".format(f, fields_id_dict[f])
                              for f in non_const_fields])
            body = "\n".join([2 * tab + "this->{0} = {1};".format(d, default_dict[d])
                              for d in default_fields]) + "\n" + body
            const_init = ":" + ",".join(["{0}(arg{1})".format(f, fields_id_dict[f])
                                         for f in const_fields])

        if lang == DST_LANGUAGE.JAVA:
            body = "\n".join([2 * tab + "this.{0} = arg{1};".format(f, i)
                              for i, f in enumerate(all_constructor_params)])
            body = "\n".join([2 * tab + "this.{0} = {1};".format(d, default_dict[d])
                              for d in default_fields if default_dict[d] is not None]) + "\n" + body

            if len(all_constructor_params) > 0:
                code += "protected " + cls.__name__ + "() {}\n\n" + tab

        code += "public " + cls.__name__ + "(" + args + ")" + const_init + "{"
        if body != "":
            code += "\n" + body + "\n" + tab
        code += "}\n\n"

        return code

    def build_fields_dict(self):
        fields = { name : self.__getattribute__(name)
                   for name in dir(self)
                   if isinstance(self.__getattribute__(name), BasicASTNodeField) }
        node = self
        while hasattr(node, "superclass"):
            node = node.superclass()
            superfields = [(attr_name, node.__getattribute__(attr_name)) for attr_name in dir(node)]
            for name, value in superfields:
                if name not in fields and isinstance(value, BasicASTNodeField):
                    fields[name] = value

        return fields

    def gen_deep_copy(self, lang):
        """Generate the code of the method that creates a deep clone of an AST node.

        Returns the code of the method that creates a new instance of the same class and fills
        fields of this new instance according to deep copy mode of each field.
        """
        if lang == DST_LANGUAGE.CPP:
            return self.gen_deep_copy_cpp()
        elif lang == DST_LANGUAGE.JAVA:
            return self.gen_deep_copy_java()
        else:
            raise Exception("unexpected destination language {0}".format(lang))

    def gen_deep_copy_cpp(self):
        # FIXME
        raise NotImplementedError

    def gen_deep_copy_java(self):
        cls = self.__class__
        classname = cls.__name__

        all_fields = dict(cls.__field_types)
        all_fields.update(cls.__super_field_types)

        body_indent = 2 * tab
        fields_assigns = []
        insert_empty_line = True

        for name, field_data in sorted(all_fields.items(), key=lambda x : x[0]):
            field_lines = field_data[3].generate_code(DST_LANGUAGE.JAVA, "copy", "skipConstantFunCalls", "nodesMap")

            if insert_empty_line or len(field_lines) > 1:
                fields_assigns.append("")

            insert_empty_line = len(field_lines) > 1
            fields_assigns.extend(field_lines)

        fields_assigns = map(lambda s : body_indent + s if len(s) > 0 else "", fields_assigns)

        if classname in unique_nodes:
            body = body_indent + "if (skipConstantFunCalls) {\n"
            body += body_indent + tab + "if (nodesMap.isPresent()) {\n"
            body += body_indent + 2 * tab + "nodesMap.get().put(this, this);\n"
            body += body_indent + tab + "}\n"
            body += body_indent + tab + "return this;\n"
            body += body_indent + "}\n\n"
        else:
            body = ""

        body += body_indent + "final {0} copy = new {0}();\n".format(classname)
        body += "\n".join(fields_assigns) + "\n"
        body += "\n" + body_indent + "if (nodesMap.isPresent()) {\n"
        body += body_indent + tab + "checkArgument(!nodesMap.get().containsKey(this), \"the given map contains this node\");\n"
        body += body_indent + tab + "nodesMap.get().put(this, copy);\n"
        body += body_indent + "}\n"
        body += "\n" + body_indent + "return copy;\n"

        override_attr = tab + "@Override\n" if hasattr(self, "superclass") else ""

        code = override_attr + tab + "public {0} deepCopy() {{\n".format(classname)
        code += 2 * tab + "return deepCopy(false);\n"
        code += tab + "}\n\n"

        code += override_attr + tab + "public {0} deepCopy(boolean skipConstantFunCalls) {{\n".format(classname)
        code += 2 * tab + "return deepCopy(skipConstantFunCalls, Optional.<Map<Node, Node>>absent());\n"
        code += tab + "}\n\n"

        code += override_attr + tab + "public {0} deepCopy(boolean skipConstantFunCalls, "\
                    "Optional<Map<Node, Node>> nodesMap) {{\n".format(classname)
        code += body
        code += tab + "}\n\n"

        return code

    def gen_substitute(self, lang):
        if lang == DST_LANGUAGE.CPP:
            return self.gen_substitute_cpp()
        elif lang == DST_LANGUAGE.JAVA:
            return self.gen_substitute_java()
        else:
            raise Exception("unexpected destination language '{0}".format(lang))

    def gen_substitute_cpp(self):
        # FIXME
        raise NotImplementedError

    def gen_substitute_java(self):
        body_lines = []

        for name, field in map(lambda x: (x, self.__getattribute__(x)), dir(self)):
            if not isinstance(field, BasicASTNodeField):
                continue

            field_lines = field.gen_subst_code(DST_LANGUAGE.JAVA, name, ast_nodes.keys(),
                                               generic_nodes, "manager")
            if len(field_lines) > 0:
                if len(body_lines) > 0:
                    body_lines.append("")
                body_lines.append("// {0}".format(name))
                body_lines.extend(field_lines)

        body_lines = map(lambda s: 2 * tab + s if len(s) != 0 else "", body_lines)
        body = "\n".join(body_lines)

        if len(body) == 0 and hasattr(self, "superclass"):
            return ""
        elif len(body) == 0:
            return tab + "public void substitute(SubstitutionManager manager) {}\n\n"
        else:
            body += "\n"

        if hasattr(self, "superclass"):
            code = tab + "@Override\n"
            code += tab + "public void substitute(SubstitutionManager manager) {\n"
            code += 2 * tab + "super.substitute(manager);\n\n"
        else:
            code = tab + "public void substitute(SubstitutionManager manager) {\n"

        code += body
        code += tab + "}\n\n"

        return code

    def gen_set_paste_flag_deep(self, lang):
        if lang == DST_LANGUAGE.CPP:
            return self.gen_set_paste_flag_deep_cpp()
        elif lang == DST_LANGUAGE.JAVA:
            return self.gen_set_paste_flag_deep_java()
        else:
            raise Exception("unexpected destination language '{0}'".format(lang))

    def gen_set_paste_flag_deep_cpp(self):
        # FIXME
        raise NotImplementedError

    def gen_set_paste_flag_deep_java(self):
        classname = self.__class__.__name__
        body_lines = ["this.isPasted = value;"]

        for name, field in map(lambda x: (x, self.__getattribute__(x)), dir(self)):
            if not isinstance(field, BasicASTNodeField):
                continue

            field_lines = field.gen_dfs_calls(DST_LANGUAGE.JAVA, name, ast_nodes.keys(), "setIsPastedFlagDeep", "value")

            if len(field_lines) > 0:
                if len(body_lines) > 0:
                    body_lines.append("")
                body_lines.append("// {0}".format(name))
                body_lines.extend(field_lines)

        if len(body_lines) == 0 and hasattr(self, "superclass"):
            return ""
        elif len(body_lines) == 0:
            return tab + "public void setIsPastedFlagDeep(boolean value) {}\n\n"

        body = 2 * tab + "super.setIsPastedFlagDeep(value);\n\n" if hasattr(self, "superclass") else ""
        body_lines = map(lambda s : 2 * tab + s if len(s) > 0 else "", body_lines)
        body += "\n".join(body_lines) + "\n"

        code = tab + "@Override\n" if hasattr(self, "superclass") else ""
        code += tab + "public void setIsPastedFlagDeep(boolean value) {\n"
        code += body
        code += tab + "}\n\n"

        return code

    def gen_remangling_code(self, lang, names_map, mangler):
        if lang == DST_LANGUAGE.JAVA:
            return self.gen_remangling_code_java(names_map, mangler)
        elif lang == DST_LANGUAGE.CPP:
            return self.gen_remangling_code_cpp(names_map, mangler)
        else:
            raise Exception("unexpected destination language '{0}'".format(lang))

    def gen_remangling_code_cpp(self, names_map, mangler):
        # FIXME
        raise NotImplementedError

    def gen_remangling_code_java(self, names_map, mangler):
        classname = self.__class__.__name__
        header = ""
        body = []

        # Add a call to the method for the superclass
        if hasattr(self, "superclass"):
            header = "this.visit{0}(node, arg);\n".format(self.superclass().__class__.__name__)

        # Perform the remangling if necessary
        if classname in mangle_nodes:
            indicator = mangle_nodes[classname][1]
            getter_name = "get{0}".format(first_to_cap(indicator.unique_field_name))
            setter_name = "set{0}".format(first_to_cap(indicator.unique_field_name))
            flag_getter_name = "get{0}".format(first_to_cap(indicator.remangle_flag_name))

            if not indicator.optional:
                body.append("if (node.{0}() != null && node.{1}()) {{".format(getter_name, flag_getter_name))
                get_expr = "node.{0}()".format(getter_name)
            else:
                body.append("if (node.{0}() != null && node.{0}().isPresent() && node.{1}()) {{".format(getter_name, flag_getter_name))
                get_expr = "node.{0}().get()".format(getter_name)

            body.append(tab + "final Optional<String> storedName = Optional.fromNullable(this.{0}.get({1}));"
                        .format(names_map, get_expr))
            body.append(tab + "final String mangledName = storedName.isPresent()")
            body.append(2 * tab + "? storedName.get()")
            body.append(2 * tab + ": this.{0}.apply({1});".format(mangler, get_expr))
            body.append(tab + "if (!storedName.isPresent()) {")
            body.append(2 * tab + "this.{0}.put({1}, mangledName);".format(names_map, get_expr))
            body.append(tab + "}")

            if not indicator.optional:
                body.append(tab + "node.{0}(mangledName);".format(setter_name))
            else:
                body.append(tab + "node.{0}(Optional.of(mangledName));".format(setter_name))

            body.append("}")

        # Mangle names in children nodes
        for name, field in [(name, self.__getattribute__(name)) for name in dir(self)]:
            if isinstance(field, BasicASTNodeField):
                new_lines = field.gen_mangling_code(DST_LANGUAGE.JAVA, name, ast_nodes.keys(), "node")
                if len(new_lines) > 0 and len(body) > 0:
                    body.append("")
                body.extend(new_lines)

        # Increase the indentation of the body part
        body = [2 * tab + line if len(line) > 0 else "" for line in body]

        code = tab + "@Override\n"
        code += tab + "public Void visit{0}({0} node, Void arg) {{\n".format(classname)
        code += 2 * tab + header if len(header) > 0 else ""
        code += "\n" if len(header) > 0 and len(body) > 0 else ""
        code += "\n".join(body) + "\n" if len(body) > 0 else ""
        code += "\n" if len(body) > 0 else ""
        code += 2 * tab + "return null;\n"
        code += tab + "}\n\n"

        return code

    def get_fields(self, lang):
        fields = []
        field_types = dict()
        field_defaults = dict()
        cls = self.__class__

        if not hasattr(self, "__field_names"):
            if hasattr(self, "superclass"):
                fields, field_types, field_defaults = self.superclass().get_fields(lang)

            for n, el in map(lambda x: (x, self.__getattribute__(x)), dir(self)):
                if isinstance(el, BasicASTNodeField):
                    fields.append(n)
                    field_copy_gen = get_field_copy_gen(n, el)
                    field_types[n] = (el.const, el.get_type(lang), el.constructor_variable, field_copy_gen)
                    field_defaults[n] = el.default_value
        else:
            fields = cls.__field_names + cls.__super_field_names
            field_types = dict(cls.__field_types)
            field_types.update(cls.__super_field_types)
            field_defaults = dict(cls.__field_defaults)
            field_defaults.update(cls.__super_field_defaults)

        return fields, field_types, field_defaults

    def gen_visitable_code(self, lang):
        return self.gen_visit_code(lang) + "\n" + self.gen_traverse_code(lang)

    def gen_visit_code(self, lang):
        header = ""
        if lang == DST_LANGUAGE.JAVA:
            header = tab + "@Override\n"
            header += tab + "public <R, A> R accept(Visitor<R, A> v, A arg) {\n"
            header += tab * 2 + "return v."

        if lang == DST_LANGUAGE.CPP:
            header = tab + "virtual void accept(Visitor * v) {\n"
            header += tab * 2 + "v->"

        return header + "visit" + self.__class__.__name__ + "(this, arg);\n" + tab + "}\n"

    def gen_traverse_code(self, lang):
        return language_dispatch(lang, self.gen_traverse_code_java, self.gen_traverse_code_cpp)

    def gen_traverse_code_cpp(self):
        #FIXME
        raise NotImplementedError

    def gen_traverse_code_java(self):
        # Build the fields dictionary
        fields = self.build_fields_dict()

        # Process all fields
        body = []
        for name, fielddesc in fields.items():
            newlines = fielddesc.gen_dfs_calls(DST_LANGUAGE.JAVA, name, ast_nodes.keys(), "traverse", "visitor", "newArg")
            if newlines:
                if body:
                    body.append("")
                body.extend(newlines)

        visit_call = "visitor.visit{0}(this, arg)".format(self.__class__.__name__)

        if body:
            body[0:0] = ["final A newArg = {0};".format(visit_call), ""]
            body.extend(["", "return newArg;"])
        else:
            body = ["return {0};".format(visit_call)]

        # Increase indentation
        body = [2 * tab + line if line else "" for line in body]

        code = tab + "@Override\n"
        code += tab + "public <A> A traverse(Visitor<A, A> visitor, A arg) {\n"
        code += "\n".join(body) + "\n" if body else ""
        code += tab + "}\n"

        return code

    def gen_transform_code(self, lang, class_name, method_name, arg_class_name,
                           transform_ancestors=False, pass_source=False):
        return language_dispatch(lang, self.gen_transform_code_java,
                    self.gen_transform_code_cpp, class_name,
                    method_name, arg_class_name, transform_ancestors,
                    pass_source)

    def gen_transform_code_cpp(self, class_name, method_name, arg_class_name,
                               transform_ancestors, pass_source):
        # FIXME
        raise NotImplementedError

    def gen_transform_code_java(self, class_name, method_name, arg_class_name,
                                transform_ancestors, pass_source):
        node_class = self.__class__.__name__
        fields = self.build_fields_dict()
        fields_code = []

        for name, fielddesc in fields.items():
            field_code = fielddesc.gen_transform_code(DST_LANGUAGE.JAVA,
                            "node", name, class_name, method_name, "arg",
                            transform_ancestors, pass_source)
            fields_code.extend(field_code)

        if not fields_code:
            return ""

        fields_code = [tab * 2 + line if line else "" for line in fields_code]

        code = tab + "@Override\n"
        code += tab + "public A visit{0}({0} node, {1} arg) {{\n".format(node_class, arg_class_name)
        code += "\n".join(fields_code)
        code += "\n" + tab * 2 + "return arg;\n"
        code += tab + "}\n\n"

        return code

    def gen_field_printer_code(self, lang):
        res = self.superclass().gen_field_printer_code(lang) if hasattr(self, "superclass") else ""

        for el in map(lambda x: self.__getattribute__(x), dir(self)):
            if isinstance(el, BasicASTNodeField) and el.visitable:
                res += tab * 2 + el.gen_printer(lang) + "\n"

        return res

    def gen_printer_code(self, lang):
        header = ""
        name = self.__class__.__name__
        if lang == DST_LANGUAGE.JAVA:
            header = tab + "@Override\n"
            header += tab + "public Void visit" + name + "(" + name + " elem, Object arg) {\n"
            header += tab * 2 + "System.out.printf(\"({0}: \");\n".format(name)

        if lang == DST_LANGUAGE.CPP:
            header = tab + "virtual void visit" + name + "(" + name + "* elem) {\n"

        header += self.gen_field_printer_code(lang)
        header += tab * 2 + "System.out.printf(\"); \");\n"

        return header + tab*2 + "return null;\n" + tab + "}\n"

    def generate_code(self, lang):
        cls = self.__class__
        class_name = first_to_cap(cls.__name__)
        cls.__name__ = class_name

        reference_types = set()  # set of all classes that need to be imported
        constructor = ""
        superclass = None
        superclass_name = ""
        res = None

        sup_fld_names, sup_fld_types, sup_fld_defaults = ([], dict(), dict())

        if hasattr(self, "superclass"):
            if lang == DST_LANGUAGE.CPP:
                superclass = ": public "
            if lang == DST_LANGUAGE.JAVA:
                superclass = " extends "

            if issubclass(self.superclass, BasicASTNode):
                superclass_name = self.superclass().__class__.__name__
                superclass += superclass_name
            else:
                #here we assume that if the superclass is not a object it must be
                #a string. other values are invalid
                superclass += self.superclass
                self.superclass = ast_nodes[self.superclass]

            if not hasattr(self, "__super_field_names"):
                sup_fld_names, sup_fld_types, sup_fld_defaults = self.superclass().get_fields(lang)

        if not hasattr(self, "__super_field_names"):
            #we add information about the super fields to the class
            cls.__super_field_names = sup_fld_names
            cls.__super_field_types = sup_fld_types
            cls.__super_field_defaults = sup_fld_defaults

        if lang == DST_LANGUAGE.CPP:
            superclass = superclass if superclass else ": public Visitable"
        if lang == DST_LANGUAGE.JAVA:
            superclass = superclass if superclass else " implements Visitable "

        if not hasattr(self, "__field_names"):
            cls.__field_names = []
            cls.__field_types = {}
            cls.__field_defaults = {}
            cls.__fields = []  # list of strings containing code describing the fields
            cls.__methods = []  # list of strings describing all methods (getters and setters)

            members = filter(lambda x: x in dir(self), self.members__)

            for n, el in map(lambda x: (x, self.__getattribute__(x)), members):
                if isinstance(el, BasicASTNodeField):
                    field_copy_gen = get_field_copy_gen(n, el)
                    cls.__field_names.append(n)
                    cls.__field_types[n] = (el.const, el.get_type(lang), el.constructor_variable, field_copy_gen)
                    if el.default_value is not None:
                        cls.__field_defaults[n] = el.default_value
                    if el.name is None:
                        el.set_name(n)
                    if isinstance(el, (ReferenceField, ReferenceListField)):
                        reference_types.add(el.ref_type)
                    if isinstance(el, (EnumField, EnumListField)):
                        reference_types.add(el.enum_type)
                    fld, method = el.generate_code(lang)
                    cls.__fields.append(tab + fld)
                    cls.__methods.append("\n".join(map(lambda x: tab + x if x != "\n" else x, method.split("\n"))))

        constructor = self.gen_constructor(lang)
        deep_copy_method = self.gen_deep_copy(lang)
        substitute_method = self.gen_substitute(lang)
        set_paste_flag_deep_method = self.gen_set_paste_flag_deep(lang)

        if lang == DST_LANGUAGE.CPP:
            res = "class " + class_name + superclass + " {\n"
            if len(cls.__fields):
                res += "protected:\n"
                res += "\n".join(cls.__fields) + "\n\n"
            res += "public:\n"
            res += constructor
            res += "\n".join(cls.__methods) + "\n"
            res += self.gen_visitable_code(lang)
            res += "};\n"

        if lang == DST_LANGUAGE.JAVA:
            # There is no need to explicitly import classes from the same
            # package. The same with java.lang.String.
            res = "package pl.edu.mimuw.nesc.ast.gen;\n\n"
            res += "import java.math.BigInteger;\n"
            res += "import java.util.LinkedList;\n"
            res += "import java.util.ListIterator;\n"
            res += "import java.util.Map;\n"
            res += "import java.util.Set;\n"
            res += "import com.google.common.base.Optional;\n"
            res += "import com.google.common.collect.ImmutableList;\n"
            res += "import pl.edu.mimuw.nesc.ast.*;\n"
            res += "import pl.edu.mimuw.nesc.astutil.AstUtils;\n"
            res += "import pl.edu.mimuw.nesc.declaration.label.*;\n"
            res += "import pl.edu.mimuw.nesc.declaration.nesc.*;\n"
            res += "import pl.edu.mimuw.nesc.declaration.object.*;\n"
            res += "import pl.edu.mimuw.nesc.declaration.tag.*;\n"
            res += "import pl.edu.mimuw.nesc.environment.*;\n"
            res += "import pl.edu.mimuw.nesc.facade.component.specification.ModuleTable;\n"
            res += "import pl.edu.mimuw.nesc.type.*;\n\n"
            res += "import static com.google.common.base.Preconditions.checkArgument;\n"

            # Include docstring if present.
            if cls.__doc__ is not None:
                res += "\n/**\n"
                res += cls.__doc__
                res += "\n*/"

            res += "\npublic class " + class_name + superclass + " {\n"
            if len(cls.__fields):
                res += "\n".join(cls.__fields) + "\n\n"
            res += constructor
            res += "\n".join(cls.__methods) + "\n"
            res += deep_copy_method
            res += substitute_method
            res += set_paste_flag_deep_method
            res += self.gen_visitable_code(lang)
            res += "}\n"

        return res


class GenericIndicator:
    """Class that marks a node as important for generic substitution."""
    pass


class MangleIndicator:
    """Class that marks a node as containing information of name mangling."""

    def __init__(self, unique_field_name, remangle_flag_name, activated=True, optional=False):
        self.unique_field_name = unique_field_name
        self.remangle_flag_name = remangle_flag_name
        self.activated = activated
        self.optional = optional


class UniqueIndicator:
    """Class that marks a node as representing a call to a NesC constant function."""
    pass


#TODO: add comments and cleanup code
def gen_printer(lang, directory):
    if lang == DST_LANGUAGE.CPP:
        gen_cpp_printer(directory)
    if lang == DST_LANGUAGE.JAVA:
        gen_java_printer(directory)


def gen_java_printer(directory):
    printer = "package pl.edu.mimuw.nesc.ast.gen;\n\n"
    printer += "public class Printer implements Visitor<Void, Object> {\n"

    #visitor begin
    for cl in ast_nodes.keys():
        printer += ast_nodes[cl]().gen_printer_code(DST_LANGUAGE.JAVA)
        printer += "\n"
    #visitor end

    printer += "}\n"

    f = open(path.join(directory, "Printer.java"), "w")
    f.write(printer)
    f.close()


def gen_cpp_printer(directory):
    printer = "#ifndef __AST_PRINTER__\n"
    printer += "#define __AST_PRINTER__\n\n"
    printer += "#include \"AST_Node.h\"\n\n"

    printer += "class Printer: public Visitor {\n"

    #visitor begin
    for cl in ast_nodes.keys():
        printer += ast_nodes[cl]().gen_printer_code(DST_LANGUAGE.CPP)
        printer += "\n"
    #visitor end

    printer += "};\n"
    printer += "#endif\n"

    f = open(path.join(directory, "printer.hpp"), "w")
    f.write(printer)
    f.close()


def gen_visitor(lang, directory):
    if lang == DST_LANGUAGE.CPP:
        return gen_cpp_visitor(directory)
    if lang == DST_LANGUAGE.JAVA:
        gen_java_visitor(directory)


def gen_cpp_visitor(directory):
    visitor = "class Visitor {\n"
    visitor += "public:\n"
    visitor += tab + "virtual ~Visitor() {}\n"
    for cl in ast_nodes.keys():
        visitor += tab + "virtual void visit" + cl + "(" + cl + " elem) = 0;\n"
    visitor += "};\n\n"

    return visitor


def gen_java_visitor(directory):
    gen_java_visitable(directory)
    gen_java_visitor_interface(directory)
    gen_java_exception_visitor(directory)
    gen_java_null_visitor(directory)
    gen_java_identity_visitor(directory)
    gen_java_decorator_visitor(directory)


def gen_java_visitable(directory):
    visitable = "package pl.edu.mimuw.nesc.ast.gen;\n\n"
    visitable += "public interface Visitable {\n"
    visitable += tab + "<R, A> R accept(Visitor<R, A> v, A arg);\n"
    visitable += tab + "<A> A traverse(Visitor<A, A> v, A arg);\n"
    visitable += "}\n"

    f = open(path.join(directory, "Visitable.java"), "w")
    f.write(visitable)
    f.close()


def gen_java_visitor_interface(directory):
    visitor = "package pl.edu.mimuw.nesc.ast.gen;\n\n"
    visitor += "public interface Visitor <R, A> {\n"
    for cl in ast_nodes.keys():
        visitor += tab + "R visit" + cl + "(" + cl + " elem, A arg);\n"
    visitor += "}\n"

    f = open(path.join(directory, "Visitor.java"), "w")
    f.write(visitor)
    f.close()


def gen_java_exception_visitor(directory):
    exception_visitor = "package pl.edu.mimuw.nesc.ast.gen;\n\n"
    exception_visitor += "public abstract class ExceptionVisitor <R, A> implements Visitor <R, A> {\n"
    for cl in ast_nodes.keys():
        exception_visitor += tab + "public R visit" + cl + "(" + cl + " elem, A arg) {\n"
        exception_visitor += tab * 2 + "throw new UnsupportedOperationException(\"The visitor for class "
        exception_visitor += cl + " is not implemented\");\n"
        exception_visitor += tab + "}\n"
    exception_visitor += "}\n"

    f = open(path.join(directory, "ExceptionVisitor.java"), "w")
    f.write(exception_visitor)
    f.close()


def gen_java_null_visitor(directory):
    with open(path.join(directory, "NullVisitor.java"), "w") as f:
        f.write("package pl.edu.mimuw.nesc.ast.gen;\n\n")
        f.write("public abstract class NullVisitor<R, A> implements Visitor<R, A> {\n")
        methods = []

        for classname in ast_nodes.keys():
            method = tab + "public R visit{0}({0} node, A arg) {{\n".format(classname)
            method += tab * 2 + "return null;\n"
            method += tab + "}"
            methods.append(method)

        f.write("\n\n".join(methods))
        f.write("\n}\n")


def gen_java_identity_visitor(directory):
    with open(path.join(directory, "IdentityVisitor.java"), "w") as f:
        f.write("package pl.edu.mimuw.nesc.ast.gen;\n\n")
        f.write("public abstract class IdentityVisitor<A> implements Visitor<A, A> {\n")
        first = True

        for classname in ast_nodes.keys():
            if not first:
                f.write("\n")
            first = False
            f.write(tab + "public A visit{0}({0} node, A arg) {{\n".format(classname))
            f.write(tab * 2 + "return arg;\n")
            f.write(tab + "}\n")

        f.write("}\n")


def gen_java_decorator_visitor(directory):
    with open(path.join(directory, "DecoratorVisitor.java"), "w") as f:
        f.write("package pl.edu.mimuw.nesc.ast.gen;\n\n")
        f.write("import static com.google.common.base.Preconditions.checkNotNull;\n\n")
        f.write("public abstract class DecoratorVisitor<R, A> implements Visitor<R, A> {\n")
        f.write(tab + "private final Visitor<R, A> decoratedVisitor;\n\n")
        f.write(tab + "protected DecoratorVisitor(Visitor<R, A> decoratedVisitor) {\n")
        f.write(tab * 2 + 'checkNotNull(decoratedVisitor, "decorated visitor cannot be null");\n')
        f.write(tab * 2 + "this.decoratedVisitor = decoratedVisitor;\n")
        f.write(tab + "}\n\n")
        f.write(tab + "protected Visitor<R, A> getDecoratedVisitor() {\n")
        f.write(tab * 2 + "return decoratedVisitor;\n")
        f.write(tab + "}\n")

        for classname in ast_nodes.keys():
            f.write("\n")
            f.write(tab + "public R visit{0}({0} node, A arg) {{\n".format(classname))
            f.write(tab * 2 + "return decoratedVisitor.visit{0}(node, arg);\n".format(classname))
            f.write(tab + "}\n")

        f.write("}\n")


def gen_subst_manager(lang, directory):
    if lang == DST_LANGUAGE.CPP:
        gen_subst_manager_cpp(directory)
    elif lang == DST_LANGUAGE.JAVA:
        gen_subst_manager_java(directory)
    else:
        raise Exception("unexpected destination language '{0}'".format(lang))


def gen_subst_manager_cpp(directory):
    # FIXME
    raise NotImplementedError


def gen_subst_manager_java(directory):
    with open(path.join(directory, "SubstitutionManager.java"), "w") as f:
        f.write("package pl.edu.mimuw.nesc.ast.gen;\n\n")
        f.write("import com.google.common.base.Optional;\n\n")
        f.write("public interface SubstitutionManager {\n")

        lines = [ tab + "Optional<{0}> substitute({0} node);".format(classname)
                  for classname in generic_nodes.keys()]
        f.write("\n".join(lines) + "\n")

        f.write("}\n")


def gen_expr_transformation(lang, directory):
    language_dispatch(lang, gen_expr_transformation_java,
                gen_expr_transformation_cpp, directory)


def gen_expr_transformation_cpp(directory):
    # FIXME
    raise NotImplementedError


def gen_expr_transformation_java(directory):
    with open(path.join(directory, "ExprTransformation.java"), 'w') as f:
        f.write("package pl.edu.mimuw.nesc.ast.gen;\n\n")
        f.write("import java.util.LinkedList;\n\n")
        f.write("public interface ExprTransformation<A> {\n")
        f.write(tab + "LinkedList<Expression> transform(Expression expr, A arg);\n")
        f.write("}\n")


def gen_expr_transformer(lang, directory):
    language_dispatch(lang, gen_expr_transformer_java, gen_expr_transformer_cpp,
                      directory)


def gen_expr_transformer_cpp(directory):
    # FIXME
    raise NotImplementedError


def gen_expr_transformer_java(directory):
    with open(path.join(directory, "ExprTransformer.java"), "w") as f:
        f.write("package pl.edu.mimuw.nesc.ast.gen;\n\n")
        f.write("import com.google.common.base.Optional;\n")
        f.write("import java.util.Iterator;\n")
        f.write("import java.util.LinkedList;\n")
        f.write("import java.util.ListIterator;\n")
        f.write("import pl.edu.mimuw.nesc.ast.Location;\n\n")
        f.write("import static com.google.common.base.Preconditions.checkNotNull;\n\n")
        f.write("public class ExprTransformer<A> extends IdentityVisitor<A> {\n")
        f.write(tab + "private final ExprTransformation<A> transformation;\n\n")
        f.write(tab + "public ExprTransformer(ExprTransformation<A> transformation) {\n")
        f.write(tab * 2 + 'checkNotNull(transformation, "transformation cannot be null");\n')
        f.write(tab * 2 + 'this.transformation = transformation;\n')
        f.write(tab + "}\n\n")

        for node_cls in ast_nodes.values():
            f.write(node_cls().gen_transform_code(DST_LANGUAGE.JAVA,
                    "Expression", "performTransformation", "A"))

        gen_expr_transformer_ref_methods_java(f)
        gen_expr_transformer_list_methods_java(f)
        f.write("}\n")


def gen_expr_transformer_ref_methods_java(f):
    f.write(tab + "private Expression performTransformation(Expression expr, A arg) {\n")
    f.write(tab * 2 + "if (expr == null) {\n")
    f.write(tab * 3 + "return null;\n")
    f.write(tab * 2 + "}\n\n")
    f.write(tab * 2 + "final LinkedList<Expression> result = transformation.transform(expr, arg);\n\n")
    f.write(tab * 2 + "if (result.isEmpty()) {\n")
    f.write(tab * 3 + 'throw new RuntimeException("expression transformed to an empty list");\n')
    f.write(tab * 2 + "} else if (result.size() == 1) {\n")
    f.write(tab * 3 + "return result.getFirst();\n")
    f.write(tab * 2 + "} else {\n")
    f.write(tab * 3 + "final Comma comma = new Comma(Location.getDummyLocation(), result);\n")
    f.write(tab * 3 + "comma.setType(result.getLast().getType());\n")
    f.write(tab * 3 + "comma.setIsLvalue(false);\n")
    f.write(tab * 3 + "comma.setParenthesesCount(1);\n")
    f.write(tab * 3 + "return comma;\n")
    f.write(tab * 2 + "}\n")
    f.write(tab + "}\n\n")
    f.write(tab + "private Optional<Expression> performTransformation(Optional<Expression> expr, A arg) {\n")
    f.write(tab * 2 + "if (expr == null) {\n")
    f.write(tab * 3 + "return null;\n")
    f.write(tab * 2 + "}\n\n")
    f.write(tab * 2 + "return expr.isPresent()\n")
    f.write(tab * 3 + "? Optional.of(performTransformation(expr.get(), arg))\n")
    f.write(tab * 3 + ": Optional.<Expression>absent();\n")
    f.write(tab + "}\n\n")


def gen_expr_transformer_list_methods_java(f):
    f.write(tab + "private void performTransformations(LinkedList<Expression> exprs, A arg) {\n")
    f.write(tab * 2 + "if (exprs == null) {\n")
    f.write(tab * 3 + "return;\n")
    f.write(tab * 2 + "}\n\n")
    f.write(tab * 2 + 'final ListIterator<Expression> exprsIt = exprs.listIterator();\n\n')
    f.write(tab * 2 + "while (exprsIt.hasNext()) {\n")
    f.write(tab * 3 + "exprsIt.set(performTransformation(exprsIt.next(), arg));\n")
    f.write(tab * 2 + "}\n")
    f.write(tab + "}\n\n")
    f.write(tab + "private void performTransformations(Optional<LinkedList<Expression>> exprs, A arg) {\n")
    f.write(tab * 2 + "if (exprs != null && exprs.isPresent()) {\n")
    f.write(tab * 3 + "performTransformations(exprs.get(), arg);\n")
    f.write(tab * 2 + "}\n")
    f.write(tab + "}\n")


def gen_stmt_transformation(lang, directory):
    language_dispatch(lang, gen_stmt_transformation_java,
                      gen_stmt_transformation_cpp, directory)


def gen_stmt_transformation_cpp(directory):
    # FIXME
    raise NotImplementedError


def gen_stmt_transformation_java(directory):
    with open(path.join(directory, "StmtTransformation.java"), "w") as f:
        f.write("package pl.edu.mimuw.nesc.ast.gen;\n\n")
        f.write("import java.util.LinkedList;\n\n")
        f.write("public interface StmtTransformation<A> {\n")
        f.write(tab + "LinkedList<Statement> transform(Statement stmt, A arg);\n")
        f.write("}\n")


def gen_stmt_transformer(lang, directory):
    language_dispatch(lang, gen_stmt_transformer_java,
                      gen_stmt_transformer_cpp, directory)


def gen_stmt_transformer_cpp(directory):
    # FIXME
    raise NotImplementedError


def gen_stmt_transformer_java(directory):
    with open(path.join(directory, "StmtTransformer.java"), "w") as f:
        f.write("package pl.edu.mimuw.nesc.ast.gen;\n\n")
        f.write("import com.google.common.base.Optional;\n")
        f.write("import java.util.Iterator;\n")
        f.write("import java.util.LinkedList;\n")
        f.write("import java.util.ListIterator;\n")
        f.write("import pl.edu.mimuw.nesc.common.util.list.Lists;\n")
        f.write("import pl.edu.mimuw.nesc.ast.Location;\n\n")
        f.write("import static com.google.common.base.Preconditions.checkNotNull;\n\n")
        f.write("public class StmtTransformer<A> extends IdentityVisitor<A> {\n")
        f.write(tab + "private final StmtTransformation<A> transformation;\n\n")
        f.write(tab + "public StmtTransformer(StmtTransformation<A> transformation) {\n")
        f.write(tab * 2 + 'checkNotNull(transformation, "transformation cannot be null");\n')
        f.write(tab * 2 + "this.transformation = transformation;\n")
        f.write(tab + "}\n\n")

        for node_cls in ast_nodes.values():
            f.write(node_cls().gen_transform_code(DST_LANGUAGE.JAVA,
                    "Statement", "performTransformation", "A"))

        gen_stmt_transformer_ref_methods_java(f)
        gen_stmt_transformer_list_methods_java(f)
        f.write("}\n")


def gen_stmt_transformer_ref_methods_java(f):
    f.write(tab + "private Statement performTransformation(Statement stmt, A arg) {\n")
    f.write(tab * 2 + "if (stmt == null) {\n")
    f.write(tab * 3 + "return null;\n")
    f.write(tab * 2 + "}\n\n")
    f.write(tab * 2 + "final LinkedList<Statement> result = transformation.transform(stmt, arg);\n\n")
    f.write(tab * 2 + "if (result.isEmpty()) {\n")
    f.write(tab * 3 + "return new EmptyStmt(Location.getDummyLocation());\n")
    f.write(tab * 2 + "} else if (result.size() == 1) {\n")
    f.write(tab * 3 + "return result.getFirst();\n")
    f.write(tab * 2 + "} else {\n")
    f.write(tab * 3 + "return new CompoundStmt(\n")
    f.write(tab * 4 + "Location.getDummyLocation(),\n")
    f.write(tab * 4 + "Lists.<IdLabel>newList(),\n")
    f.write(tab * 4 + "Lists.<Declaration>newList(),\n")
    f.write(tab * 4 + "result\n")
    f.write(tab * 3 + ");\n")
    f.write(tab * 2 + "}\n")
    f.write(tab + "}\n\n")
    f.write(tab + "private Optional<Statement> performTransformation(Optional<Statement> stmt, A arg) {\n")
    f.write(tab * 2 + "if (stmt == null) {\n")
    f.write(tab * 3 + "return null;\n")
    f.write(tab * 2 + "}\n\n")
    f.write(tab * 2 + "return stmt.isPresent()\n")
    f.write(tab * 3 + "? Optional.of(performTransformation(stmt.get(), arg))\n")
    f.write(tab * 3 + ": Optional.<Statement>absent();\n")
    f.write(tab + "}\n\n")


def gen_stmt_transformer_list_methods_java(f):
    f.write(tab + "private void performTransformations(LinkedList<Statement> stmts, A arg) {\n")
    f.write(tab * 2 + "if (stmts == null) {\n")
    f.write(tab * 3 + "return;\n")
    f.write(tab * 2 + "}\n\n")
    f.write(tab * 2 + "final ListIterator<Statement> stmtsIt = stmts.listIterator();\n\n")
    f.write(tab * 2 + "while (stmtsIt.hasNext()) {\n")
    f.write(tab * 3 + "final LinkedList<Statement> newStmts = transformation.transform(stmtsIt.next(), arg);\n\n")
    f.write(tab * 3 + "if (newStmts.isEmpty()) {\n")
    f.write(tab * 4 + "stmtsIt.remove();\n")
    f.write(tab * 3 + "} else {\n")
    f.write(tab * 4 + "final Iterator<Statement> newStmtsIt = newStmts.iterator();\n")
    f.write(tab * 4 + "stmtsIt.set(newStmtsIt.next());\n\n")
    f.write(tab * 4 + "while (newStmtsIt.hasNext()) {\n")
    f.write(tab * 5 + "stmtsIt.add(newStmtsIt.next());\n")
    f.write(tab * 4 + "}\n")
    f.write(tab * 3 + "}\n")
    f.write(tab * 2 + "}\n")
    f.write(tab + "}\n\n")
    f.write(tab + "private void performTransformations(Optional<LinkedList<Statement>> stmts, A arg) {\n")
    f.write(tab * 2 + "if (stmts != null && stmts.isPresent()) {\n")
    f.write(tab * 3 + "performTransformations(stmts.get(), arg);\n")
    f.write(tab * 2 + "}\n")
    f.write(tab + "}\n")


def gen_attr_transformation(lang, directory):
    language_dispatch(lang, gen_attr_transformation_java,
                      gen_attr_transformation_cpp, directory)


def gen_attr_transformation_cpp(directory):
    # FIXME
    raise NotImplementedError

def gen_attr_transformation_java(directory):
    with open(path.join(directory, "AttrTransformation.java"), "w") as f:
        f.write("package pl.edu.mimuw.nesc.ast.gen;\n\n")
        f.write("import java.util.LinkedList;\n\n")
        f.write("public interface AttrTransformation<A> {\n")
        f.write(tab + "LinkedList<Attribute> transform(GccAttribute attr, Node containingNode, A arg);\n")
        f.write(tab + "LinkedList<Attribute> transform(TargetAttribute attr, Node containingNode, A arg);\n")
        f.write(tab + "LinkedList<Attribute> transform(NescAttribute attr, Node containingNode, A arg);\n")
        f.write("}\n")


def gen_attr_transformer(lang, directory):
    language_dispatch(lang, gen_attr_transformer_java,
                      gen_attr_transformer_cpp, directory)


def gen_attr_transformer_cpp(directory):
    # FIXME
    raise NotImplementedError


def gen_attr_transformer_java(directory):
    with open(path.join(directory, "AttrTransformer.java"), "w") as f:
        f.write("package pl.edu.mimuw.nesc.ast.gen;\n\n")
        f.write("import com.google.common.base.Optional;\n")
        f.write("import java.util.Iterator;\n")
        f.write("import java.util.LinkedList;\n")
        f.write("import java.util.ListIterator;\n\n")
        f.write("import static com.google.common.base.Preconditions.checkNotNull;\n\n")
        f.write("public class AttrTransformer<A> extends IdentityVisitor<A> {\n")
        f.write(tab + "private final AttrTransformation<A> transformation;\n\n")
        f.write(tab + "private final Visitor<LinkedList<Attribute>, TransformData<A>> dispatchingVisitor = new ExceptionVisitor<LinkedList<Attribute>, TransformData<A>>() {\n")
        f.write(tab * 2 + "@Override\n")
        f.write(tab * 2 + "public LinkedList<Attribute> visitGccAttribute(GccAttribute attr, TransformData<A> data) {\n")
        f.write(tab * 3 + "return transformation.transform(attr, data.containingNode, data.arg);\n")
        f.write(tab * 2 + "}\n")
        f.write(tab * 2 + "@Override\n")
        f.write(tab * 2 + "public LinkedList<Attribute> visitTargetAttribute(TargetAttribute attr, TransformData<A> data) {\n")
        f.write(tab * 3 + "return transformation.transform(attr, data.containingNode, data.arg);\n")
        f.write(tab * 2 + "}\n")
        f.write(tab * 2 + "@Override\n")
        f.write(tab * 2 + "public LinkedList<Attribute> visitNescAttribute(NescAttribute attr, TransformData<A> data) {\n")
        f.write(tab * 3 + "return transformation.transform(attr, data.containingNode, data.arg);\n")
        f.write(tab * 2 + "}\n")
        f.write(tab + "};\n\n")
        f.write(tab + "public AttrTransformer(AttrTransformation<A> transformation) {\n")
        f.write(tab * 2 + 'checkNotNull(transformation, "transformation cannot be null");\n')
        f.write(tab * 2 + "this.transformation = transformation;\n")
        f.write(tab + "}\n\n")

        for node_cls in ast_nodes.values():
            f.write(node_cls().gen_transform_code(DST_LANGUAGE.JAVA,
                "Attribute", "performTransformation", "A", transform_ancestors=True,
                pass_source=True))

        gen_attr_transformer_dispatch_java(f)
        gen_attr_transformer_list_methods_java(f)
        gen_attr_transformer_container_java(f)
        f.write("}\n")


def gen_attr_transformer_dispatch_java(f):
    f.write(tab + "private Optional<LinkedList<Attribute>> dispatchForTransformation(Object object, Node containingNode, A arg) {\n")
    f.write(tab * 2 + "return object instanceof Attribute\n")
    f.write(tab * 3 + "? Optional.of(((Attribute) object).accept(dispatchingVisitor, new TransformData<>(containingNode, arg)))\n")
    f.write(tab * 3 + ": Optional.<LinkedList<Attribute>>absent();\n")
    f.write(tab + "}\n\n")


def gen_attr_transformer_list_methods_java(f):
    f.write(tab + "private void performTransformations(LinkedList<? super Attribute> attrs, Node containingNode, A arg) {\n")
    f.write(tab * 2 + "if (attrs == null) {\n")
    f.write(tab * 3 + "return;\n")
    f.write(tab * 2 + "}\n\n")
    f.write(tab * 2 + "final ListIterator<? super Attribute> attrsIt = attrs.listIterator();\n\n")
    f.write(tab * 2 + "while (attrsIt.hasNext()) {\n")
    f.write(tab * 3 + "final Optional<LinkedList<Attribute>> newAttrs = dispatchForTransformation(attrsIt.next(), containingNode, arg);\n\n")
    f.write(tab * 3 + "if (!newAttrs.isPresent()) {\n")
    f.write(tab * 4 + "continue;\n")
    f.write(tab * 3 + "}\n\n")
    f.write(tab * 3 + "if (newAttrs.get().isEmpty()) {\n")
    f.write(tab * 4 + "attrsIt.remove();\n")
    f.write(tab * 3 + "} else {\n")
    f.write(tab * 4 + "final Iterator<Attribute> newAttrsIt = newAttrs.get().iterator();\n")
    f.write(tab * 4 + "attrsIt.set(newAttrsIt.next());\n\n")
    f.write(tab * 4 + "while (newAttrsIt.hasNext()) {\n")
    f.write(tab * 5 + "attrsIt.add(newAttrsIt.next());\n")
    f.write(tab * 4 + "}\n")
    f.write(tab * 3 + "}\n")
    f.write(tab * 2 + "}\n")
    f.write(tab + "}\n\n")
    f.write(tab + "private void performTransformations(Optional<? extends LinkedList<? super Attribute>> attrs, Node containingNode, A arg) {\n")
    f.write(tab * 2 + "if (attrs != null && attrs.isPresent()) {\n")
    f.write(tab * 3 + "performTransformations(attrs.get(), containingNode, arg);\n")
    f.write(tab * 2 + "}\n")
    f.write(tab + "}\n\n")


def gen_attr_transformer_container_java(f):
    f.write(tab + "private static final class TransformData<A> {\n")
    f.write(tab * 2 + "private final Node containingNode;\n")
    f.write(tab * 2 + "private final A arg;\n\n")
    f.write(tab * 2 + "private TransformData(Node containingNode, A arg) {\n")
    f.write(tab * 3 + 'checkNotNull(containingNode, "containing node cannot be null");\n')
    f.write(tab * 3 + "this.containingNode = containingNode;\n")
    f.write(tab * 3 + "this.arg = arg;\n")
    f.write(tab * 2 + "}\n")
    f.write(tab + "}\n")


def gen_remangling_visitor(lang, directory):
    if lang == DST_LANGUAGE.JAVA:
        gen_remangling_visitor_java(directory)
    elif lang == DST_LANGUAGE.CPP:
        gen_remangling_visitor_cpp(directory)
    else:
        raise Exception("unexpected destination language '{0}'".format(lang))


def gen_remangling_visitor_cpp(directory):
    # FIXME
    raise NotImplementedError


def gen_remangling_visitor_java(directory):
    with open(path.join(directory, "RemanglingVisitor.java"), "w") as f:
        f.write("package pl.edu.mimuw.nesc.ast.gen;\n\n")
        f.write("import com.google.common.base.Function;\n")
        f.write("import com.google.common.base.Optional;\n")
        f.write("import java.util.HashMap;\n")
        f.write("import java.util.Map;\n")
        f.write("\n")
        f.write("import static com.google.common.base.Preconditions.checkNotNull;\n")
        f.write("\n")
        f.write("public final class RemanglingVisitor implements Visitor<Void, Void> {\n")
        f.write(tab + "private final Map<String, String> namesMap = new HashMap<>();\n")
        f.write(tab + "private final Function<String, String> remangler;\n\n")
        f.write(tab + "public RemanglingVisitor(Function<String, String> remangler) {\n")
        f.write(2 * tab + 'checkNotNull(remangler, "the remangling function cannot be null");\n')
        f.write(2 * tab + "this.remangler = remangler;\n")
        f.write(tab + "}\n\n")
        f.write(tab + "public Map<String, String> getNamesMap() {\n")
        f.write(2 * tab + "return namesMap;\n")
        f.write(tab + "}\n\n")

        # Generate code for each node
        for node_cls in ast_nodes.values():
            f.write(node_cls().gen_remangling_code(DST_LANGUAGE.JAVA, "namesMap", "remangler"))

        f.write("}\n")


def generate_code(lang, directory=""):
    #if dir is None, then the files will be generated in the
    #current working directory. Else dir should be a valid relative
    #or absolute path leading to the directory in which the files are to be written
    if lang == DST_LANGUAGE.CPP:
        generate_cpp_code(directory)
    if lang == DST_LANGUAGE.JAVA:
        generate_java_code(directory)


def generate_cpp_code(directory):
    ast_file = "#ifndef __AST_DEF__\n"
    ast_file += "#define __AST_DEF__\n"
    ast_file += "#include <vector>\n"
    ast_file += "#include <string>\n"
    ast_file += "#include <boost/shared_ptr.hpp>\n"
    ast_file += "#include \"visitor.h\"\n"
    ast_file += "typedef char ast_int8;\n"
    ast_file += "typedef short ast_int16;\n"
    ast_file += "typedef long ast_int32;\n"
    ast_file += "typedef long long ast_int64;\n\n"
    ast_file += gen_visitor(DST_LANGUAGE.CPP, directory)
    ast_file += "class Visitable {\n"
    ast_file += "public:\n"
    ast_file += tab + "virtual ~Visitable() {}\n"
    ast_file += tab + "virtual void accept(Visitor*) = 0;\n"
    ast_file += "};\n\n"
    for k in ast_nodes.keys():
        ast_file += "class " + k + ";\n"
    ast_file += "\n"
    for el in ast_enums.values():
        ast_file += el().generate_code(DST_LANGUAGE.CPP)
    for el in ast_nodes.values():
        ast_file += el().generate_code(DST_LANGUAGE.CPP)
        ast_file += "\n"
    ast_file += "#endif"

    f = open(path.join(directory, "AST_Nodes.hpp"), "w")
    f.write(ast_file)
    f.close()

    gen_printer(DST_LANGUAGE.CPP, directory)


def generate_java_code(directory):
    for d in (ast_nodes, ast_enums):
        for k in d.keys():
            ast_file = d[k]().generate_code(DST_LANGUAGE.JAVA)

            if not path.exists(directory):
                makedirs(directory)

            f = open(path.join(directory, k + ".java"), "w")
            f.write(ast_file)
            f.close()

    gen_visitor(DST_LANGUAGE.JAVA, directory)

    gen_printer(DST_LANGUAGE.JAVA, directory)

    gen_subst_manager(DST_LANGUAGE.JAVA, directory)

    gen_remangling_visitor(DST_LANGUAGE.JAVA, directory)

    gen_expr_transformation(DST_LANGUAGE.JAVA, directory)

    gen_expr_transformer(DST_LANGUAGE.JAVA, directory)

    gen_stmt_transformation(DST_LANGUAGE.JAVA, directory)

    gen_stmt_transformer(DST_LANGUAGE.JAVA, directory)

    gen_attr_transformation(DST_LANGUAGE.JAVA, directory)

    gen_attr_transformer(DST_LANGUAGE.JAVA, directory)

