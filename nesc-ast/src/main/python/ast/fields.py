from ast.util import first_to_cap, DST_LANGUAGE, tab, ast_nodes
from ast.field_copy import DEEP_COPY_MODE

#the basic class from which all field classes must derive.
#All field classes must have name and const fields
class BasicASTNodeField:
    def __init__(self, constructor_variable=True, default_value=None, visitable=True, optional=False,
                 deep_copy_mode=DEEP_COPY_MODE.ASSIGN_DEEP_COPY):
        self.constructor_variable = constructor_variable
        self.default_value = default_value
        self.visitable = visitable
        self.optional = optional
        self.deep_copy_mode = deep_copy_mode

    #expected result for c++ and java is a pair of strings
    #that represent the member declaration and getter/setter functions
    def generate_code(self, lang):
        raise NotImplementedError

    #expected result for c++ and java is a string
    #representing valid code for printing the field
    def gen_printer(self, lang):
        raise NotImplementedError

    #expected result for c++ and java is a string
    #representing the full type of the field
    def get_type(self, lang, full=False):
        raise NotImplementedError

    def set_name(self, name):
        self.name = name

    def gen_getter_code(self, lang, type):
        code = None

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
    def __init__(self, name=None, const=False, deep_copy_mode=DEEP_COPY_MODE.ASSIGN_REFERENCE_COPY,
                 *args, **kwargs):
        super(BoolField, self).__init__(deep_copy_mode=deep_copy_mode, *args, **kwargs)
        self.name = name
        self.const = const

    def get_type(self, lang, full=False):
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = "bool"

            if self.const and full:
                res = "const " + res

        if lang == DST_LANGUAGE.JAVA:
            res = "Boolean"

            if self.optional:
                res = "Optional<Boolean>"

            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: %s\\n", "{0}", elem->get{0}() ? "true":"false");'

        if lang == DST_LANGUAGE.JAVA:
            res = 'if (elem.get{0}() != null) {{\n'
            res += tab * 3 + 'System.out.printf("%s: %s; ", "{0}", elem.get{0}());\n'
            res += tab * 2 + '}} else {{\n'
            res += tab * 3 + 'System.out.printf("%s : null; ", "{0}");\n'
            res += tab * 2 + '}}'

        return res.format(first_to_cap(self.name))

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
        res = None

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
            if self.width == INT_TYPE.LONG:
                res = "Long"
            else:
                res = "Integer"

            if self.optional:
                res = "Optional<"
                if self.width == INT_TYPE.LONG:
                    res += "Long"
                else:
                    res += "Integer"
                res += ">"

            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        res = None

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
            res = 'if (elem.get{0}() != null) {{\n'
            res += tab * 3 + 'System.out.printf("%s: %s; ", "{0}", elem.get{0}());\n'
            res += tab * 2 + '}} else {{\n'
            res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
            res += tab * 2 + '}}'

        return res.format(first_to_cap(self.name))

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
        res = None

        if self.width == FLOAT_TYPE.SINGLE:
            res = "float"
        if self.width == FLOAT_TYPE.DOUBLE:
            res = "double"

        if lang == DST_LANGUAGE.CPP:
            if self.const and full:
                res = "const " + res

        if lang == DST_LANGUAGE.JAVA:
            res = first_to_cap(res)
            if self.optional:
                res = "Optional<"
                if self.width == FLOAT_TYPE.SINGLE:
                    res += "Float"
                if self.width == FLOAT_TYPE.DOUBLE:
                    res += "Double"
                res += ">"
            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: %f\\n", "{0}", elem->get{0}());'

        if lang == DST_LANGUAGE.JAVA:
            res = 'if (elem.get{0}() != null) {{\n'
            res += tab * 3 + 'System.out.printf("%s: %s; ", "{0}", elem.get{0}());\n'
            res += tab * 2 + '}} else {{\n'
            res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
            res += tab * 2 + '}}'

        return res.format(first_to_cap(self.name))

    def generate_code(self, lang):
        res = self.get_type(lang, True)

        code = self.gen_getter_code(lang, res)

        if lang == DST_LANGUAGE.CPP:
            res += " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + res + " " + self.name + ";"

        return res, code


class StringField(BasicASTNodeField):
    def __init__(self, name=None, const=False, deep_copy_mode=DEEP_COPY_MODE.ASSIGN_REFERENCE_COPY,
                 *args, **kwargs):
        super(StringField, self).__init__(deep_copy_mode=deep_copy_mode, *args, **kwargs)
        self.name = name
        self.const = const

    def get_type(self, lang, full=False):
        res = None
        if lang == DST_LANGUAGE.CPP:
            res = "std::string"

            if self.const and full:
                res = "const " + res

        if lang == DST_LANGUAGE.JAVA:
            res = "String"

            if self.optional:
                res = "Optional<String>"

            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        res = None
        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: \\"%s\\"\\n", "{0}", elem->get{0}().c_str());'

        if lang == DST_LANGUAGE.JAVA:
            res = 'if (elem.get{0}() != null) {{\n'
            res += tab * 3 + 'System.out.printf("%s: %s; ", "{0}", elem.get{0}());\n'
            res += tab * 2 + '}} else {{\n'
            res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
            res += tab * 2 + '}}'

        return res.format(first_to_cap(self.name))

    def generate_code(self, lang):
        res = None
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
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = "boost::shared_ptr<" + self.ref_type + ">"

            if self.const and full:
                res = "const " + res

        if lang == DST_LANGUAGE.JAVA:
            res = self.ref_type
            if self.optional:
                res = "Optional<" + self.ref_type + ">"

            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: {{\\n", "{0}");\n'
            res += tab * 2 + "elem->get{0}()->accept(this);\n"
            res += tab * 2 + 'printf("}}\\n");'

        if lang == DST_LANGUAGE.JAVA:
            if not self.optional:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'System.out.printf("%s: {{", "{0}");\n'
                if self.ref_type in ast_nodes.keys():
                    res += tab * 3 + 'elem.get{0}().accept(this, arg);\n'
                else:
                    res += tab * 3 + 'System.out.printf("%s; ", elem.get{0}().toString());\n'
                res += tab * 3 + 'System.out.printf("}}; ");\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
                res += tab * 2 + '}}'
            else:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'if (elem.get{0}().isPresent()) {{\n'
                res += tab * 4 + 'System.out.printf("%s: {{", "{0}");\n'
                if self.ref_type in ast_nodes.keys():
                    res += tab * 4 + 'elem.get{0}().get().accept(this, arg);\n'
                else:
                    res += tab * 4 + 'System.out.printf(elem.get{0}().get().toString());\n'
                res += tab * 4 + 'System.out.printf("}}; ");\n'
                res += tab * 3 + '}} else {{\n'
                res += tab * 4 + 'System.out.printf("%s: null; ", "{0}");\n'
                res += tab * 3 + '}}\n'
                res += tab * 2 + '}}'

        return res.format(first_to_cap(self.name))

    def generate_code(self, lang):
        res = None

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
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = self.enum_type

            if self.const and full:
                res = "const " + res

        if lang == DST_LANGUAGE.JAVA:
            res = self.enum_type
            if self.optional:
                res = "Optional<" + self.enum_type + ">"

            if self.const and full:
                res = "final " + res

        return res

    def gen_printer(self, lang):
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = 'enum_printer_' + self.enum_type + '(elem->get{0}())\n'

        if lang == DST_LANGUAGE.JAVA:
            res = 'if (elem.get{0}() != null) {{\n'
            res += tab * 3 + 'System.out.printf("%s: %s; ", "{0}", elem.get{0}());'
            res += tab * 2 + '}} else {{\n'
            res += tab * 3 + 'System.out.printf("%s: null; ", {0});\n'
            res += tab * 2 + '}}'

        return res.format(first_to_cap(self.name))

    def generate_code(self, lang):
        res = None

        code = self.gen_getter_code(lang, self.get_type(lang))
        if lang == DST_LANGUAGE.CPP:
            res = self.get_type(lang, True) + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + self.get_type(lang, True) + " " + self.name + ";"

        return res, code


class BoolListField(BasicASTNodeField):
    def __init__(self, name=None, const=False, *args, **kwargs):
        super(BoolListField, self).__init__(*args, **kwargs)
        self.name = name
        self.const = const

    def get_type(self, lang, full=False):
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = "std::vector<bool>"

        if lang == DST_LANGUAGE.JAVA:
            res = "LinkedList<Boolean>"
            if self.optional:
                res = "Optional<" + res + ">"

        return res

    def gen_printer(self, lang):
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'
            res += tab * 3 + 'printf("%s ", elem->get{0}()[i] ? "true":"false");\n'
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            if not self.optional:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 3 + 'for(Boolean bval : elem.get{0}()) {{\n'
                res += tab * 4 + 'System.out.printf("%s ", bval);\n'
                res += tab * 3 + '}}\n'
                res += tab * 3 + 'System.out.printf("]; ");\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: []; ", "{0}");\n'
                res += tab * 2 + '}}'
            else:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'if (elem.get{0}().isPresent()) {{\n'
                res += tab * 4 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 4 + 'for(Boolean bval : elem.get{0}().get()) {{\n'
                res += tab * 5 + 'System.out.printf("%s ", bval);\n'
                res += tab * 4 + '}}\n'
                res += tab * 4 + 'System.out.printf("]; ");\n'
                res += tab * 3 + '}} else {{\n'
                res += tab * 4 + 'System.out.printf("%s: %s", "{0}",elem.get{0}());\n'
                res += tab * 3 + '}}\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
                res += tab * 2 + '}}'

        return res.format(first_to_cap(self.name))

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
        res = None

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

            if self.optional:
                res = "Optional<" + res + ">"

        return res

    def gen_printer(self, lang):
        width = "d"
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'.format(self.name)
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'.format(self.name)
            if self.width == INT_TYPE.BYTE:
                width = "hhd"
            if self.width == INT_TYPE.SHORT:
                width = "hd"
            if self.width == INT_TYPE.NORMAL:
                width = "ld"
            if self.width == INT_TYPE.LONG:
                width = "lld"
            res += tab * 3
            res += 'printf("%{1} ", elem->get{0}()[i] ? "true":"false");\n'.format(first_to_cap(self.name), width)
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            if not self.optional:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 3 + 'for({1} ival : elem.get{0}()) {{\n'
                res += tab * 4 + 'System.out.printf("%d ", ival.longValue());\n'
                res += tab * 3 + '}}\n'
                res += tab * 3 + 'System.out.printf("]; ");\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
                res += tab * 2 + '}}'
            else:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'if (elem.get{0}().isPresent()) {{\n'
                res += tab * 4 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 4 + 'for({1} ival : elem.get{0}().get()) {{\n'
                res += tab * 5 + 'System.out.printf("%d ", ival.longValue());\n'
                res += tab * 4 + '}}\n'
                res += tab * 4 + 'System.out.printf("]; ");\n'
                res += tab * 3 + '}} else {{\n'
                res += tab * 4 + 'System.out.printf("%s: %s", "{0}", elem.get{0}());\n'
                res += tab * 3 + '}}\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
                res += tab * 2 + '}}'

        return res.format(first_to_cap(self.name), "Long" if self.width == INT_TYPE.LONG else "Integer")

    def generate_code(self, lang):
        res = self.get_type(lang, True)
        code = self.gen_getter_code(lang, res)

        if lang == DST_LANGUAGE.CPP:
            res += " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + res + self.name + ";"

        return res, code


class FloatListField(BasicASTNodeField):
    def __init__(self, name=None, width=FLOAT_TYPE.SINGLE, const=False, *args, **kwargs):
        super(FloatListField, self).__init__(*args, **kwargs)
        self.name = name
        self.width = width
        self.const = const

    def get_type(self, lang, full=False):
        res = None

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
            if self.optional:
                res = "Optional<" + res + ">"

        return res

    def gen_printer(self, lang):
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'
            res += tab * 3 + 'printf("%f ", elem->get{0}()[i]);\n'
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            if not self.optional:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 3 + 'for({1} fval : elem.get{0}()) {{\n'
                res += tab * 4 + 'System.out.printf("%s ", fval);\n'
                res += tab * 3 + '}}\n'
                res += tab * 3 + 'System.out.printf("]; ");\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
                res += tab * 2 + '}}'
            else:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'if (elem.get{0}().isPresent()) {{\n'
                res += tab * 4 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 4 + 'for({1} fval : elem.get{0}().get()) {{\n'
                res += tab * 5 + 'System.out.printf("%s ", fval);\n'
                res += tab * 4 + '}}\n'
                res += tab * 4 + 'System.out.printf("]; ");\n'
                res += tab * 3 + '}} else {{\n'
                res += tab * 4 + 'System.out.printf("%s: %s", "{0}", elem.get{0}());\n'
                res += tab * 3 + '}}\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
                res += tab * 2 + '}}'

        return res.format(first_to_cap(self.name), "Float" if self.width == FLOAT_TYPE.SINGLE else "Double")

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
        res = None
        if lang == DST_LANGUAGE.CPP:
            res = "std::vector<std::string>"

        if lang == DST_LANGUAGE.JAVA:
            res = "LinkedList<String>"
            if self.optional:
                res = "Optional<" + res + ">"

        return res

    def gen_printer(self, lang):
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'
            res += tab * 3 + 'printf("%s ", elem->get{0}()[i].c_str());\n'
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            if not self.optional:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 3 + 'for(String sval : elem.get{0}()) {{\n'
                res += tab * 4 + 'System.out.printf("%s ", sval);\n'
                res += tab * 3 + '}}\n'
                res += tab * 3 + 'System.out.printf("]; ");\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
                res += tab * 2 + '}}'
            else:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'if (elem.get{0}().isPresent()) {{\n'
                res += tab * 4 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 4 + 'for(String sval : elem.get{0}().get()) {{\n'
                res += tab * 5 + 'System.out.printf("%s ", sval);\n'
                res += tab * 4 + '}}\n'
                res += tab * 4 + 'System.out.printf("]; ");\n'
                res += tab * 3 + '}} else {{\n'
                res += tab * 4 + 'System.out.printf("%s: %s", "{0}", elem.get{0}());\n'
                res += tab * 3 + '}}\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
                res += tab * 2 + '}}'

        return res.format(first_to_cap(self.name))

    def generate_code(self, lang):
        res = None
        res_type = None
        if lang == DST_LANGUAGE.CPP:
            res_type = "std::vector<std::string>"
            res = res_type + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res_type = self.get_type(lang)
            res = "protected " + res_type + " " + self.name + ";"

        code = self.gen_getter_code(lang, res_type)

        return res, code


class ReferenceListField(BasicASTNodeField):
    def __init__(self, ref_type, name=None, const=False, deep_copy_mode=DEEP_COPY_MODE.ASSIGN_LIST_DEEP_COPY,
                 *args, **kwargs):
        super(ReferenceListField, self).__init__(deep_copy_mode=deep_copy_mode, *args, **kwargs)
        self.name = name
        self.const = const

        if not isinstance(ref_type, str):
            self.ref_type = ref_type().__class__.__name__
        else:
            self.ref_type = ref_type

        self.ref_type = first_to_cap(self.ref_type)

    def get_type(self, lang, full=False):
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = "std::vector<boost::shared_ptr<" + self.ref_type + "> >"

        if lang == DST_LANGUAGE.JAVA:
            res = "LinkedList<" + self.ref_type + ">"
            if self.optional:
                res = "Optional<" + res + ">"

        return res

    def gen_printer(self, lang):
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'
            res += tab * 3 + 'elem->get{0}()[i]->accept(this);\n'
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            if not self.optional:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 3 + 'for({1} rval : elem.get{0}()) {{\n'
                res += tab * 4 + 'if (rval != null) {{\n'
                if self.ref_type in ast_nodes.keys():
                    res += tab * 5 + 'rval.accept(this, arg);\n'
                else:
                    res += tab * 5 + 'System.out.printf(rval.toString());\n'
                res += tab * 4 + '}} else {{\n'
                res += tab * 5 + 'System.out.printf("null ");\n'
                res += tab * 4 + '}}\n'
                res += tab * 3 + '}}\n'
                res += tab * 3 + 'System.out.printf("]; ");\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
                res += tab * 2 + '}}'
            else:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'if (elem.get{0}().isPresent()) {{\n'
                res += tab * 4 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 4 + 'for({1} rval : elem.get{0}().get()) {{\n'
                res += tab * 5 + 'if (rval != null) {{\n'
                if self.ref_type in ast_nodes.keys():
                    res += tab * 6 + 'rval.accept(this, arg);\n'
                else:
                    res += tab * 6 + 'System.out.printf(rval.toString());\n'
                res += tab * 5 + '}} else {{\n'
                res += tab * 6 + 'System.out.printf("null ");\n'
                res += tab * 5 + '}}\n'
                res += tab * 4 + '}}\n'
                res += tab * 4 + 'System.out.printf("]; ");\n'
                res += tab * 3 + '}} else {{\n'
                res += tab * 4 + 'System.out.printf("%s", elem.get{0}());\n'
                res += tab * 3 + '}}\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", "{0}");\n'
                res += tab * 2 + '}}'

        return res.format(first_to_cap(self.name), self.ref_type)

    def generate_code(self, lang):
        res = None
        res_type = None

        if lang == DST_LANGUAGE.CPP:
            res_type = "std::vector<boost::shared_ptr<" + self.ref_type + "> >"
            res = res_type + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res_type = self.get_type(lang)
            res = "protected " + res_type + " " + self.name + ";"

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
        res = None

        if lang == DST_LANGUAGE.CPP:
            res = "std::vector<" + self.enum_type + ">"

        if lang == DST_LANGUAGE.JAVA:
            res = "LinkedList<" + self.enum_type + ">"
            if self.optional:
                res = "Optional<" + res + ">"

        return res

    def gen_printer(self, lang):
        res = ""

        if lang == DST_LANGUAGE.CPP:
            res = 'printf("%s: [", "{0}");'
            res += tab * 2 + 'for(int i=0; i<elem->get{0}.size(); ++i) {{\n'
            res += tab * 3 + 'enum_printer_' + self.enum_type + '(elem->get{0}()[i])\n'
            res += tab * 2 + '}}\n'
            res += tab * 2 + 'printf("]\\n");'

        if lang == DST_LANGUAGE.JAVA:
            if not self.optional:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 3 + 'for({1}} eval : elem.get{0}()) {{\n'
                res += tab * 4 + 'System.out.printf("%s ", eval);\n'
                res += tab * 3 + '}}\n'
                res += tab * 3 + 'System.out.printf("]; ");\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", {0})\n'
                res += tab * 2 + '}}'
            else:
                res = 'if (elem.get{0}() != null) {{\n'
                res += tab * 3 + 'if (elem.get{0}().isPresent()) {{\n'
                res += tab * 4 + 'System.out.printf("%s: [", "{0}");\n'
                res += tab * 4 + 'for({1} eval : elem.get{0}().get()) {{\n'
                res += tab * 5 + 'System.out.printf("%s ", eval);\n'
                res += tab * 4 + '}}\n'
                res += tab * 4 + 'System.out.printf("]; ");'
                res += tab * 3 + '}} else {{\n'
                res += tab * 4 + 'System.out.printf("%s: %s", elem.get{0});\n'
                res += tab * 3 + '}}\n'
                res += tab * 2 + '}} else {{\n'
                res += tab * 3 + 'System.out.printf("%s: null; ", {0})\n'
                res += tab * 2 + '}}'

        return res.format(self.name, self.enum_type)

    def generate_code(self, lang):
        res = None
        res_type = None

        if lang == DST_LANGUAGE.CPP:
            res_type = "std::vector<" + self.enum_type + ">"
            res = res_type + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res = "protected " + self.get_type(lang) + self.name + ";"
            res_type = self.get_type(lang)

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
        res = None
        if lang == DST_LANGUAGE.CPP:
            res = "std::set<" + self.enum_type + ">"

        if lang == DST_LANGUAGE.JAVA:
            res = "EnumSet<" + self.enum_type + ">"
            if self.optional:
                res = "Optional<" + res + ">"

        return res

    def gen_printer(self, lang):
        res = ""
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
        res = None
        res_type = None

        if lang == DST_LANGUAGE.CPP:
            res_type = self.get_type(lang)
            res = res_type + " " + self.name + ";"

        if lang == DST_LANGUAGE.JAVA:
            res_type = self.get_type(lang)
            res = "protected " + res_type + " " + self.name + ";"

        code = self.gen_getter_code(lang, res_type)

        return res, code

