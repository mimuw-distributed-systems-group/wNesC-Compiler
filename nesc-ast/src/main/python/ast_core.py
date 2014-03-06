from os import path, makedirs
from collections import OrderedDict

#TODO: add standard ast options:
#   - line and colum as default fields
#   - specify package option for java
#   - option to generate a jar file
#   - ability to say that a class is virtual?
#   - add generation from bnf? (doubling functionality? what should it generate, a py file?)
#   - fields excluded from the constructor!
#   - some objects represent operators and do not have fields
#     -> add an option of a class header (both a global and a local one)
#   - optional fields in constructor?
#     -> Probobly not as we would be forced to generate all the combinantions
#     -> only reasonable solution is to be able to mark k first fields as a single option
#     -> problem with that: superclasses
#   - functions to push_front or push_back to a *ListField
#   - any thing else?

#A dictionary of all nodes of the abstract syntax tree
ast_nodes = OrderedDict()
ast_enums = OrderedDict()

#defines the width of the indentation in the generated code
tab = " " * 4


#a small utility function for capitalizing class names
def first_to_cap(str):
    return str[0].capitalize() + str[1:]


#since python lacks enums we use classes instead
#DST_LANGUAGE represents the target language for the generation of code
class DST_LANGUAGE:
    CPP = 0
    JAVA = 1


#This metaclass is used for registering ast nodes in our dictionary
#to make the generation process easier
class ASTElemMetaclass(type):
    @classmethod
    def __prepare__(metacls, name, bases, **kwds):
        return OrderedDict()

    def __new__(cls, name, bases, namespace, **kwds):
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
                              for d in default_fields])
            const_init = ":" + ",".join(["{0}(arg{1})".format(f, fields_id_dict[f])
                                         for f in const_fields])

        if lang == DST_LANGUAGE.JAVA:
            body = "\n".join([2 * tab + "this.{0} = arg{1};".format(f, i)
                              for i, f in enumerate(all_constructor_params)])
            body = "\n".join([2 * tab + "this.{0} = {1};".format(d, default_dict[d])
                              for d in default_fields if default_dict[d] is not None]) + body

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

        return (fields, field_types, field_defaults)

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
            header += tab + "public <R, A> visit" + name + "(" + name + " elem, A arg) {\n"

        if lang == DST_LANGUAGE.CPP:
            header = tab + "virtual void visit" + name + "(" + name + "* elem) {\n"

        header += self.gen_field_printer_code(lang)

        return header + tab + "}\n"

    def generate_code(self, lang):
        cls = self.__class__
        class_name = first_to_cap(cls.__name__)
        cls.__name__ = class_name

        reference_types = set()  # set of all classes that need to be imported
        constructor = ""
        superclass = None
        superclass_name = ""

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
                superclass_name = self.superclass

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

            for n, el in map(lambda x: (x, self.__getattribute__(x)), self.members__):
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
            #res += "import java.lang.String;\n"
            res += "import java.util.LinkedList;\n"
            #res += "import pl.edu.mimuw.nesc.ast.gen.Visitable;\n"
            res += "import pl.edu.mimuw.nesc.ast.*;\n"
            res += "import pl.edu.mimuw.nesc.ast.datadeclaration.*;\n"
            #for cls_ref in reference_types:
            #	res += "import pl.edu.mimuw.nesc.ast.gen." + cls_ref + ";\n"
            res += "\npublic class " + class_name + superclass + " {\n"
            if len(cls.__fields):
                res += "\n".join(cls.__fields) + "\n\n"
            res += constructor
            res += "\n".join(cls.__methods) + "\n"
            res += self.gen_visitable_code(lang)
            res += "}\n"

        return res


#the basic class from which all field classes must derive.
#All field classes must have name and const fields
class BasicASTNodeField:
    def __init__(self, constructor_variable=True, default_value=None, visitable=True):
        self.constructor_variable = constructor_variable
        self.default_value = default_value
        self.visitable = visitable

    #expected result for c++ and java is a pair of strings
    #that represent the member declaration and getter/setter functions
    def generate_code(self, name, lang):
        raise NotImplementedError

    #expected result for c++ and java is a string
    #representing valid code for printing the field
    def gen_printer(self, name, lang):
        raise NotImplementedError

    #expected result for c++ and java is a string
    #representing the full type of the field
    def get_type(self, lang, full=False):
        raise NotImplementedError

    def set_name(self, name):
        self.name = name

    def gen_getter_code(self, lang, type):
        if lang == DST_LANGUAGE.CPP:
            code = type + " get" + first_to_cap(self.name) + "() {\n"
            code += tab + "return this->" + self.name + ";\n"
            code += "}\n"
            if not self.const:
                code += "\nvoid set" + first_to_cap(self.name) + "(" + type + " val) {\n"
                code += tab + "this->" + self.name + " = val;\n"
                code += "}\n"

        if lang == DST_LANGUAGE.JAVA:
            code = "public " + type + " get" + first_to_cap(self.name) + "() {\n"
            code += tab + "return this." + self.name + ";\n"
            code += "}\n"
            if not self.const:
                code += "\npublic void set" + first_to_cap(self.name) + "(" + type + " val) {\n"
                code += tab + "this." + self.name + " = val;\n"
                code += "}\n"

        return code


#since python has no enums we use the following classes in
#their stead
class INT_TYPE:
    BYTE = 0
    SHORT = 1
    NORMAL = 2
    LONG = 3


class FLOAT_TYPE:
    SINGLE = 0
    DOUBLE = 1


#all fields available
#TODO: add EnumField and EnumListField
class BoolField(BasicASTNodeField):
    def __init__(self, name=None, const=False, *args, **kwargs):
        super(BoolField, self).__init__(*args, **kwargs)
        self.name = name
        self.const = const

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            res = "bool"

            if self.const and full:
                res = "const " + res

        if lang == DST_LANGUAGE.JAVA:
            res = "boolean"

            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: %s\\n", "{0}", elem->get{0}() ? "true":"false");'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf("%s: %b", "{0}", elem.get{0}());'

        return res.format(self.name)

    def generate_code(self, lang):
        res = self.get_type(lang, True)

        code = self.gen_getter_code(lang, res)

        if lang == DST_LANGUAGE.CPP:
            res += " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + res + " " + self.name + ";"

        return res, code


class IntField(BasicASTNodeField):
    def __init__(self, name=None, width=INT_TYPE.NORMAL,
                 signed=True, const=False, *args, **kwargs):
        super(IntField, self).__init__(*args, **kwargs)
        self.name = name
        self.width = width
        self.signed = signed
        self.const = const

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            if self.width == INT_TYPE.BYTE:
                res = "ast_int8"
            if self.width == INT_TYPE.SHORT:
                res = "ast_int16"
            if self.width == INT_TYPE.NORMAL:
                res = "ast_int32"
            if self.width == INT_TYPE.LONG:
                res = "ast_int64"

            if self.signed:
                res = "unsigned " + res

            if self.const and full:
                res = "const " + res

        if lang == DST_LANGUAGE.JAVA:
            if self.width == INT_TYPE.BYTE:
                res = "byte"
            if self.width == INT_TYPE.SHORT:
                res = "short"
            if self.width == INT_TYPE.NORMAL:
                res = "int"
            if self.width == INT_TYPE.LONG:
                res = "long"

            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: %'
            if self.width == INT_TYPE.BYTE:
                res += "hhd"
            if self.width == INT_TYPE.SHORT:
                res += "hd"
            if self.width == INT_TYPE.NORMAL:
                res += "ld"
            if self.width == INT_TYPE.LONG:
                res += "lld"
            res += '\\n", "{0}", elem->get{0}());'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf("%s: %d\\n", "{0}", elem.get{0}());'

        return res.format(self.name)

    def generate_code(self, lang):
        res = self.get_type(lang, True)

        code = self.gen_getter_code(lang, res)

        if lang == DST_LANGUAGE.CPP:
            res += " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + res + " " + self.name + ";"

        return res, code


class FloatField(BasicASTNodeField):
    def __init__(self, name=None, width=FLOAT_TYPE.SINGLE,
                 const=False, *args, **kwargs):
        super(FloatField, self).__init__(*args, **kwargs)
        self.name = name
        self.width = width
        self.const = const

    def get_type(self, lang, full=False):
        if self.width == FLOAT_TYPE.SINGLE:
            res = "float"
        if self.width == FLOAT_TYPE.DOUBLE:
            res = "double"

        if lang == DST_LANGUAGE.CPP:
            if self.const and full:
                res = "const " + res

        if lang == DST_LANGUAGE.JAVA:
            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: %f\\n", "{0}", elem->get{0}());'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf("%s: %f\\n", "{0}", elem.get{0}());'

        return res.format(self.name)

    def generate_code(self, lang):
        res = self.get_type(lang, True)

        code = self.gen_getter_code(lang, res)

        if lang == DST_LANGUAGE.CPP:
            res += " " + self.name + ";"

        if lang == DST_LANGUAGE.CPP:
            res = "protected " + res + " " + self.name + ";"

        return res, code


class StringField(BasicASTNodeField):
    def __init__(self, name=None, const=False, *args, **kwargs):
        super(StringField, self).__init__(*args, **kwargs)
        self.name = name
        self.const = const

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            res = "std::string"

            if self.const and full:
                res = "const " + res

        if lang == DST_LANGUAGE.JAVA:
            res = "String"

            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: \\"%s\\"\\n", "{0}", elem->get{0}().c_str());'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf(\"%s: \\"%s\\"\", \"{0}\", elem.get{0}());'

        return res.format(self.name)

    def generate_code(self, lang):
        code = self.gen_getter_code(lang, self.get_type(lang, True))

        if lang == DST_LANGUAGE.CPP:
            res = self.get_type(lang, True) + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + self.get_type(lang, True) + " " + self.name + ";"

        return res, code


class ReferenceField(BasicASTNodeField):
    def __init__(self, ref_type, name=None, const=False, *args, **kwargs):
        super(ReferenceField, self).__init__(*args, **kwargs)
        self.name = name
        self.const = const

        if not isinstance(ref_type, str):
            self.ref_type = ref_type().__class__.__name__
        else:
            self.ref_type = ref_type

        self.ref_type = first_to_cap(self.ref_type)

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            res = "boost::shared_ptr<" + self.ref_type + ">"

            if self.const and full:
                res = "const " + res

        if lang == DST_LANGUAGE.JAVA:
            res = self.ref_type

            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: {{\\n", "{0}");\n'
            res += tab * 2 + "elem->get{0}()->accept(this);\n"
            res += tab * 2 + 'printf("}}\\n");'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf("%s: {{\\n", "{0}");\n'
            res += tab * 2 + 'elem.get{0}().accept(this);\n'
            res += tab * 2 + 'System.out.printf("}}\\n");'

        return res.format(self.name)

    def generate_code(self, lang):
        code = self.gen_getter_code(lang, self.get_type(lang))
        if lang == DST_LANGUAGE.CPP:
            res = self.get_type(lang, True) + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + self.get_type(lang, True) + " " + self.name + ";"

        return res, code


class EnumField(BasicASTNodeField):
    def __init__(self, enum_type, name=None, const=False, *args, **kwargs):
        super(EnumField, self).__init__(*args, **kwargs)
        self.name = name
        self.const = const

        if not isinstance(enum_type, str):
            self.enum_type = enum_type().__class__.__name__
        else:
            self.enum_type = enum_type

        self.enum_type = first_to_cap(self.enum_type)

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            res = self.enum_type

            if self.const and full:
                res = "const " + res

        if lang == DST_LANGUAGE.JAVA:
            res = self.enum_type

            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'enum_printer_' + self.enum_type + '(elem->get{0}())\n'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf(elem.get{0}());\n'

        return res.format(self.name)

    def generate_code(self, lang):
        code = self.gen_getter_code(lang, self.get_type(lang))
        if lang == DST_LANGUAGE.CPP:
            res = self.get_type(lang, True) + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + self.get_type(lang, True) + " " + self.name + ";"

        return res, code


class BoolListField(BasicASTNodeField):
    def __init__(self, name=None, width=INT_TYPE.NORMAL,
                 signed=True, const=False, *args, **kwargs):
        super(BoolListField, self).__init__(*args, **kwargs)
        self.name = name
        self.width = width
        self.signed = signed

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            res = "std::vector<bool>"

        if lang == DST_LANGUAGE.JAVA:
            res = "LinkedList<Boolean>"

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'
            res += tab * 3 + 'printf("%s ", elem->get{0}()[i] ? "true":"false");\n'
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf("%s: [", "{0}");\n'
            res += tab * 2 + 'for(boolean bval : elem.get{0}()) {{\n'
            res += tab * 3 + 'System.out.printf("%b ", bval);\n'
            res += tab * 2 + '}};\n'
            res += tab * 2 + 'System.out.printf("]\\n");'

        return res.format(self.name)

    def generate_code(self, lang):
        res = self.get_type(lang, True)
        code = self.gen_getter_code(lang, res)

        if lang == DST_LANGUAGE.CPP:
            res += " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + res + " " + self.name + ";"

        return res, code


class IntListField(BasicASTNodeField):
    def __init__(self, name=None, width=INT_TYPE.NORMAL,
                 signed=True, const=False, *args, **kwargs):
        super(IntListField, self).__init__(*args, **kwargs)
        self.name = name
        self.width = width
        self.signed = signed
        self.const = const

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            if self.width == INT_TYPE.BYTE:
                res = "ast_int8"
            if self.width == INT_TYPE.SHORT:
                res = "ast_int16"
            if self.width == INT_TYPE.NORMAL:
                res = "ast_int32"
            if self.width == INT_TYPE.LONG:
                res = "ast_int64"

            if self.signed:
                res = "std::vector<unsigned " + res + ">"
            else:
                res = "std::vector<" + res + ">"

        if lang == DST_LANGUAGE.JAVA:
            res = "LinkedList<"
            if self.width == INT_TYPE.LONG:
                res += "Long"
            else:
                res += "Integer"

            res += ">"

        return res

    def gen_printer(self, lang):
        type = "d"

        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'.format(name)
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'.format(name)
            if self.width == INT_TYPE.BYTE:
                type = "hhd"
            if self.width == INT_TYPE.SHORT:
                type = "hd"
            if self.width == INT_TYPE.NORMAL:
                type = "ld"
            if self.width == INT_TYPE.LONG:
                type = "lld"
            res += tab * 3 + 'printf("%{1} ", elem->get{0}()[i] ? "true":"false");\n'.format(name, type)
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf("%s: [", "{0}");\n'.format(name)
            res += tab * 2 + 'for({1} ival : elem.get{0}()) {{\n'.format(name,
                                                                         "Long" if self.width == INT_TYPE.LONG
                                                                         else "Integer")
            res += tab * 3 + 'System.out.printf("%d ", ival.longValue());\n'
            res += tab * 2 + '}};\n'
            res += tab * 2 + 'System.out.printf("]\\n");'

        return res

    def generate_code(self, lang):
        res = self.get_type(lang, True)
        code = self.gen_getter_code(lang, res)

        if lang == DST_LANGUAGE.CPP:
            res += " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + res + self.name + ";"

        return res, code


class FloatListField(BasicASTNodeField):
    def __init__(self, name=None, width=FLOAT_TYPE.SINGLE, const=False):
        super(FloatListField, self).__init__(*args, **kwargs)
        self.name = name
        self.width = width
        self.const = const

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            if self.width == FLOAT_TYPE.SINGLE:
                res = "std::vector<float>"
            if self.width == FLOAT_TYPE.DOUBLE:
                res = "std::vector<double>"

        if lang == DST_LANGUAGE.JAVA:
            if self.width == FLOAT_TYPE.SINGLE:
                res = "LinkedList<Float>"
            if self.width == FLOAT_TYPE.DOUBLE:
                res = "LinkedList<Double>"

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'
            res += tab * 3 + 'printf("%f ", elem->get{0}()[i]);\n'
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf("%s: [", "{0}");\n'.format(name)
            res += tab * 2 + 'for({1} fval : elem.get{0}()) {{\n'.format(name,
                                                                         "Float" if self.width == FLOAT_TYPE.SINGLE else "Double")
            res += tab * 3 + 'System.out.printf("%s ", fval);\n'
            res += tab * 2 + '}};\n'
            res += tab * 2 + 'System.out.printf("]\\n");'

        return res.format(self.name)

    def generate_code(self, lang):
        res = self.get_type(lang, True)
        code = self.gen_getter_code(lang, res)

        if lang == DST_LANGUAGE.CPP:
            res += " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + res + self.name + ";"

        return res, code


class StringListField(BasicASTNodeField):
    def __init__(self, name=None, const=False, *args, **kwargs):
        super(StringListField, self).__init__(*args, **kwargs)
        self.name = name
        self.const = const

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            res = "std::vector<std::string>"

        if lang == DST_LANGUAGE.JAVA:
            res = "LinkedList<String>"

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'
            res += tab * 3 + 'printf("%s ", elem->get{0}()[i].c_str());\n'
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf("%s: [", "{0}");\n'
            res += tab * 2 + 'for(String bval : elem.get{0}()) {{\n'
            res += tab * 3 + 'System.out.printf("%s ", bval);\n'
            res += tab * 2 + '}};\n'
            res += tab * 2 + 'System.out.printf("]\\n");'

        return res.format(self.name)

    def generate_code(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res_type = "std::vector<std::string>"
            res = res_type + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            type = "LinkedList<String>"
            res = "protected " + res_type + " " + self.name + ";"

        code = self.gen_getter_code(lang, res_type)

        return res, code


class ReferenceListField(BasicASTNodeField):
    def __init__(self, ref_type, name=None, const=False, *args, **kwargs):
        super(ReferenceListField, self).__init__(*args, **kwargs)
        self.name = name
        self.const = const

        if not isinstance(ref_type, str):
            self.ref_type = ref_type().__class__.__name__
        else:
            self.ref_type = ref_type

        self.ref_type = first_to_cap(self.ref_type)

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            res = "std::vector<boost::shared_ptr<" + self.ref_type + "> >"

        if lang == DST_LANGUAGE.JAVA:
            res = "LinkedList<" + self.ref_type + ">"

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'
            res += tab * 3 + 'elem->get{0}()[i]->accept(this);\n'
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf("%s: [\\n", "{0}");\n'
            res += tab * 2 + 'for({0} bval : elem.get{{0}}()) {{{{\n'.format(self.ref_type)
            res += tab * 3 + 'System.out.printf("{{\\n");\n'
            res += tab * 3 + 'bval.accept(this);\n'
            res += tab * 3 + 'System.out.printf("}}\\n");\n'
            res += tab * 2 + '}};\n'
            res += tab * 2 + 'System.out.printf("]\\n");'

        return res.format(self.name)

    def generate_code(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res_type = "std::vector<boost::shared_ptr<" + self.ref_type + "> >"
            res = res_type + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected LinkedList<" + self.ref_type + "> " + self.name + ";"
            res_type = "LinkedList<" + self.ref_type + ">"

        code = self.gen_getter_code(lang, res_type)

        return res, code


class EnumListField(BasicASTNodeField):
    def __init__(self, ref_type, name=None, const=False, *args, **kwargs):
        super(EnumListField, self).__init__(*args, **kwargs)
        self.name = name
        self.const = const

        if not isinstance(ref_type, str):
            self.enum_type = ref_type().__class__.__name__
        else:
            self.enum_type = ref_type

        self.enum_type = first_to_cap(self.enum_type)

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            res = "std::vector<" + self.enum_type + ">"

        if lang == DST_LANGUAGE.JAVA:
            res = "LinkedList<" + self.enum_type + ">"

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'
            res += tab * 3 + 'enum_printer_' + self.enum_type + '(elem->get{0}()[i])\n'
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf("%s: [\\n", "{0}");\n'
            res += tab * 2 + 'for({0} bval : elem.get{{0}}()) {{{{\n'.format(self.enum_type)
            res += tab * 3 + 'System.out.printf("{{\\n");\n'
            res += tab * 3 + 'bval.accept(this);\n'
            res += tab * 3 + 'System.out.printf("}}\\n");\n'
            res += tab * 2 + '}};\n'
            res += tab * 2 + 'System.out.printf("]\\n");'

        return res.format(self.name)

    def generate_code(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res_type = "std::vector<" + self.enum_type + ">"
            res = res_type + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected LinkedList<" + self.enum_type + "> " + self.name + ";"
            res_type = "LinkedList<" + self.enum_type + ">"

        code = self.gen_getter_code(lang, res_type)

        return res, code


class EnumSetField(BasicASTNodeField):
    def __init__(self, ref_type, name=None, const=False, *args, **kwargs):
        super(EnumSetField, self).__init__(*args, **kwargs)
        self.name = name
        self.const = const

        if not isinstance(ref_type, str):
            self.enum_type = ref_type().__class__.__name__
        else:
            self.enum_type = ref_type

        self.enum_type = first_to_cap(self.enum_type)

    def get_type(self, lang, full=False):
        if lang == DST_LANGUAGE.CPP:
            res = "std::set<" + self.enum_type + ">"

        if lang == DST_LANGUAGE.JAVA:
            res = "EnumSet<" + self.enum_type + ">"

        return res

    def gen_printer(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'
            res += tab * 2 + 'for(auto set_elem: elem->get{0}) {{\n'
            res += tab * 3 + 'enum_printer_' + self.enum_type + '(set_elem)\n'
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            res = 'System.out.printf("%s: %s\\n", "{0}", elem.get{0});\n'

        return res.format(self.name)

    def generate_code(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res_type = "std::set<" + self.enum_type + ">"
            res = res_type + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected EnumSet<" + self.enum_type + "> " + self.name + ";"
            res_type = "EnumSet<" + self.enum_type + ">"

        code = self.gen_getter_code(lang, res_type)

        return res, code


#TODO: add enum support in final code generation
#      all the enum printers, where do they all belong?
#      all the enum printers, where do they all come from?
#      i look at all the enum printers x2
class BasicASTEnum(metaclass=ASTElemMetaclass):
    def generate_code(self, lang):
        enum_name = first_to_cap(self.__class__.__name__)
        if lang == DST_LANGUAGE.CPP:
            res = "enum " + enum_name + "{\n"
        if lang == DST_LANGUAGE.JAVA:
            res = "package pl.edu.mimuw.nesc.ast.gen;\n\n"
            res += "public enum " + enum_name + "{\n"

        for n, el in map(lambda x: (x, self.__getattribute__(x)), dir(self)):
            if isinstance(el, EnumElement):
                if el.name is None:
                    el.name = n
                res += tab + el.generate_code(lang)

        if lang == DST_LANGUAGE.CPP:
            res += "};\n"
        if lang == DST_LANGUAGE.JAVA:
            res += "}\n"

        return res

    def gen_printer_code(self, lang):
        enum_name = first_to_cap(self.__class__.__name__)
        # we don't generate printer code for Java, as
        # the default printer for enums is just fine
        if lang == DST_LANGUAGE.CPP:
            res = "void enum_printer_{0}({0} val) {{\n"

            for n, el in map(lambda x: (x, self.__getattribute__(x)), dir(self)):
                if isinstance(el, EnumElement):
                    res += tab + el.gen_printer(lang)

            res += "}}\n\n"

            return res.format(enum_name)


class EnumElement:
    def __init__(self, name=None, value=None):
        self.name = name
        self.value = value

    def generate_code(self, lang):
        if lang == DST_LANGUAGE.CPP:
            res = self.name + (" = " + str(self.value) if self.value else "") + ",\n"
        if lang == DST_LANGUAGE.JAVA:
            res = self.name + ",\n"

        return res

    def gen_printer(self, lang):
        # we don't generate printer code for Java, as
        # the default printer for enums is just fine
        if lang == DST_LANGUAGE.CPP:
            return 'if(val == {{0}}.{0}) printf("enum  {{0}}:{0}\\n");\n'.format(self.name)
        if lang == DST_LANGUAGE.JAVA:
            #TODO: This probably dosen't work. Check how to make it work
            return None


#TODO: add comments and cleanup code
def gen_printer(lang, dir):
    if lang == DST_LANGUAGE.CPP:
        gen_cpp_printer(dir)
    if lang == DST_LANGUAGE.JAVA:
        gen_java_printer(dir)


def gen_java_printer(dir):
    printer = "package pl.edu.mimuw.nesc.ast.gen;\n\n"
    printer += "import pl.edu.mimuw.nesc.ast.gen.Visitor;\n"
    printer += "import pl.edu.mimuw.nesc.ast.gen.*;\n\n"
    printer += "import pl.edu.mimuw.nesc.ast.datadeclaration.*;\n"
    printer += "class Printer implements Visitor {\n"

    #visitor begin
    for cl in ast_nodes.keys():
        printer += ast_nodes[cl]().gen_printer_code(DST_LANGUAGE.JAVA)
        printer += "\n"
    #visitor end

    printer += "}\n"

    f = open(path.join(dir, "Printer.java"), "w")
    f.write(printer)
    f.close()


def gen_cpp_printer(dir):
    printer = "#ifndef __AST_PRINTER__\n"
    printer += "#define __AST_PRINTER__\n"
    printer += "#include \"AST_Node.h\"\n\n"

    printer += "class Printer: public Visitor {\n"

    #visitor begin
    for cl in ast_nodes.keys():
        printer += ast_nodes[cl]().gen_printer_code(DST_LANGUAGE.CPP)
        printer += "\n"
    #visitor end

    printer += "};\n"
    printer += "#endif\n"

    f = open(path.join(dir, "printer.hpp"), "w")
    f.write(printer)
    f.close()


def gen_visitor(lang, dir):
    if lang == DST_LANGUAGE.CPP:
        return gen_cpp_visitor(dir)
    if lang == DST_LANGUAGE.JAVA:
        gen_java_visitor(dir)


def gen_cpp_visitor(dir):
    visitor = "class Visitor {\n"
    visitor += "public:\n"
    visitor += tab + "virtual ~Visitor() {}\n"
    for cl in ast_nodes.keys():
        visitor += tab + "virtual void visit" + cl + "(" + cl + " elem) = 0;\n"
    visitor += "};\n\n"

    return visitor


def gen_java_visitor(dir):
    visitable = "package pl.edu.mimuw.nesc.ast.gen;\n"
    visitable += "import pl.edu.mimuw.nesc.ast.gen.Visitor;\n\n"
    visitable += "public interface Visitable {\n"
    visitable += tab + "public <R, A> R accept(Visitor<R, A> v, A arg);\n"
    visitable += "}\n"

    f = open(path.join(dir, "Visitable.java"), "w")
    f.write(visitable)
    f.close()

    visitor = "package pl.edu.mimuw.nesc.ast.gen;\n"
    visitor += "import pl.edu.mimuw.nesc.ast.gen.*;\n\n"
    visitor += "import pl.edu.mimuw.nesc.ast.datadeclaration.*;\n"
    visitor += "public abstract class Visitor <R, A> {\n"
    for cl in ast_nodes.keys():
        visitor += tab + "public R visit" + cl + "(" + cl + " elem, A arg) {\n"
        visitor += tab * 2 + "throw new UnsupportedOperationException(\"The visitor for class"
        visitor += cl + " is not implemented\");\n"
        visitor += "}\n"
    visitor += "}\n"

    f = open(path.join(dir, "Visitor.java"), "w")
    f.write(visitor)
    f.close()


def generate_code(lang, dir=""):
    #if dir is None, then the files will be generated in the
    #current working directory. Else dir should be a valid relative
    #or absolute path leading to the directory in which the files are to be written
    if lang == DST_LANGUAGE.CPP:
        generate_cpp_code(dir)
    if lang == DST_LANGUAGE.JAVA:
        generate_java_code(dir)


def generate_cpp_code(dir):
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
    ast_file += gen_visitor(DST_LANGUAGE.CPP, dir)
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

    f = open(path.join(dir, "AST_Nodes.hpp"), "w")
    f.write(ast_file)
    f.close()

    gen_printer(DST_LANGUAGE.CPP, dir)


def generate_java_code(dir):
    for d in (ast_nodes, ast_enums):
        for k in d.keys():
            ast_file = d[k]().generate_code(DST_LANGUAGE.JAVA)

            if not path.exists(dir):
                makedirs(dir)

            f = open(path.join(dir, k + ".java"), "w")
            f.write(ast_file)
            f.close()

    gen_visitor(DST_LANGUAGE.JAVA, dir)

#gen_printer(DST_LANGUAGE.JAVA, dir)
