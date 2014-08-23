from os import path, makedirs
from collections import OrderedDict

from ast.util import first_to_cap, DST_LANGUAGE, tab, ast_nodes, ast_enums
from ast.fields import BasicASTNodeField, ReferenceField, ReferenceListField, EnumField, EnumListField

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
        newclass = type.__new__(cls, name, bases, dict(namespace))
        newclass.members__ = tuple(namespace)
        if name != 'BasicASTNode' and name != 'BasicASTEnum':
            if "BasicASTNode" in map(lambda x: x.__name__, bases):
                ast_nodes[first_to_cap(name)] = newclass
            if "BasicASTEnum" in map(lambda x: x.__name__, bases):
                ast_enums[first_to_cap(name)] = newclass
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
                    field_types[n] = (el.const, el.get_type(lang), el.constructor_variable)
                    field_defaults[n] = el.default_value
        else:
            fields = cls.__field_names + cls.__super_field_names
            field_types = dict(cls.__field_types)
            field_types.update(cls.__super_field_types)
            field_defaults = dict(cls.__field_defaults)
            field_defaults.update(cls.__super_field_defaults)

        return fields, field_types, field_defaults

    def gen_visitable_code(self, lang):
        header = ""
        if lang == DST_LANGUAGE.JAVA:
            header = tab + "@Override\n"
            header += tab + "public <R, A> R accept(Visitor<R, A> v, A arg) {\n"
            header += tab * 2 + "return v."

        if lang == DST_LANGUAGE.CPP:
            header = tab + "virtual void accept(Visitor * v) {\n"
            header += tab * 2 + "v->"

        return header + "visit" + self.__class__.__name__ + "(this, arg);\n" + tab + "}\n"

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
                    cls.__field_names.append(n)
                    cls.__field_types[n] = (el.const, el.get_type(lang), el.constructor_variable)
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
            res += "import java.util.LinkedList;\n"
            res += "import com.google.common.base.Optional;\n"
            res += "import pl.edu.mimuw.nesc.ast.*;\n"
            res += "import pl.edu.mimuw.nesc.declaration.label.*;\n"
            res += "import pl.edu.mimuw.nesc.declaration.nesc.*;\n"
            res += "import pl.edu.mimuw.nesc.declaration.object.*;\n"
            res += "import pl.edu.mimuw.nesc.declaration.tag.*;\n"
            res += "import pl.edu.mimuw.nesc.environment.*;\n"

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
            res += self.gen_visitable_code(lang)
            res += "}\n"

        return res

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
    visitable = "package pl.edu.mimuw.nesc.ast.gen;\n\n"
    visitable += "public interface Visitable {\n"
    visitable += tab + "<R, A> R accept(Visitor<R, A> v, A arg);\n"
    visitable += "}\n"

    f = open(path.join(directory, "Visitable.java"), "w")
    f.write(visitable)
    f.close()

    visitor = "package pl.edu.mimuw.nesc.ast.gen;\n\n"
    visitor += "public interface Visitor <R, A> {\n"
    for cl in ast_nodes.keys():
        visitor += tab + "R visit" + cl + "(" + cl + " elem, A arg);\n"
    visitor += "}\n"

    f = open(path.join(directory, "Visitor.java"), "w")
    f.write(visitor)
    f.close()

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