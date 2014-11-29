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

    #expected result is a list of lines with code for given language
    def gen_subst_code(self, lang, field_name, nodes_names, indicators, manager_name):
        return []

    def gen_subst_code_helper(self, obj_expr, obj_type, indicator, manager_name, assign_fun):
        code = ["if ({0}.get{1}() == null || !{0}.get{1}()) {{"
                    .format(obj_expr, first_to_cap(indicator[1]))]
        code.append(tab + "final Optional<{0}> replacement = {1}.substitute({2});"
                    .format(obj_type, manager_name, obj_expr))
        code.append(tab + "if (replacement.isPresent()) {")
        code.append(2 * tab + assign_fun("replacement.get()"))
        code.append(tab + "} else {")
        code.append(2 * tab + "{0}.substitute({1});".format(obj_expr, manager_name))
        code.append(tab + "}")
        code.append("} else {")
        code.append(tab + "{0}.substitute({1});".format(obj_expr, manager_name))
        code.append("}")

        return code

    #expected result is a list of lines with code for given language
    def gen_set_paste_flag_deep(self, lang, field_name, nodes_names, value_param_name):
        return []

    def gen_mangling_code(self, lang, field_name, nodes_names, node_ref_name):
        return []

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

    def gen_subst_code(self, lang, field_name, nodes_names, indicators, manager_name):
        if lang == DST_LANGUAGE.CPP:
            return self.gen_subst_code_cpp(field_name, nodes_names, indicators, manager_name)
        elif lang == DST_LANGUAGE.JAVA:
            return self.gen_subst_code_java(field_name, nodes_names, indicators, manager_name)
        else:
            raise Exception("unexpected destination language '{0}'".format(lang))

    def gen_subst_code_cpp(self, field_name, nodes_names, indicators, manager_name):
        # FIXME
        raise NotImplementedError

    def gen_subst_code_java(self, field_name, nodes_names, indicators, manager_name):
        if not self.visitable:
            return []
        elif self.ref_type not in indicators and self.ref_type not in nodes_names:
            return []
        elif self.ref_type not in indicators and self.ref_type in nodes_names:
            if not self.optional:
                return [
                    "if (this.{0} != null) {{".format(field_name),
                    tab + "this.{0}.substitute({1});".format(field_name, manager_name),
                    "}"
                ]
            else:
                return [
                    "if (this.{0} != null && this.{0}.isPresent()) {{".format(field_name),
                    tab + "this.{0}.get().substitute({1});".format(field_name, manager_name),
                    "}"
                ]

        indicator = indicators[self.ref_type]
        code = []

        if not self.optional:
            code.append("if (this.{0} != null) {{".format(field_name))
            obj_expr = "this.{0}".format(field_name)
            assign_fun = lambda s : "this.{0} = {1};".format(field_name, s)
        else:
            code.append("if (this.{0} != null && this.{0}.isPresent()) {{".format(field_name))
            obj_expr = "this.{0}.get()".format(field_name)
            assign_fun = lambda s : "this.{0} = Optional.of({1});".format(field_name, s)

        helper_code = self.gen_subst_code_helper(obj_expr, self.ref_type, indicator, manager_name, assign_fun)
        helper_code = map(lambda s : tab + s, helper_code)
        code.extend(helper_code)
        code.append("}")

        return code

    def gen_set_paste_flag_deep(self, lang, field_name, nodes_names, value_param_name):
        if lang == DST_LANGUAGE.CPP:
            return self.gen_set_paste_flag_deep_cpp(field_name, nodes_names, value_param_name)
        elif lang == DST_LANGUAGE.JAVA:
            return self.gen_set_paste_flag_deep_java(field_name, nodes_names, value_param_name)

    def gen_set_paste_flag_deep_cpp(self, field_name, nodes_names, value_param_name):
        # FIXME
        raise NotImplementedError

    def gen_set_paste_flag_deep_java(self, field_name, nodes_names, value_param_name):
        if not self.visitable or self.ref_type not in nodes_names:
            return []

        if not self.optional:
            code = ["if (this.{0} != null) {{".format(field_name)]
            node_expr = "this.{0}".format(field_name)
        else:
            code = ["if (this.{0} != null && this.{0}.isPresent()) {{".format(field_name)]
            node_expr = "this.{0}.get()".format(field_name)

        code.append(tab + "{0}.setPastedFlagDeep({1});".format(node_expr, value_param_name))
        code.append("}")

        return code

    def gen_mangling_code(self, lang, field_name, nodes_names, node_ref_name):
        if lang == DST_LANGUAGE.JAVA:
            return self.gen_mangling_code_java(field_name, nodes_names, node_ref_name)
        elif lang == DST_LANGUAGE.CPP:
            return self.gen_mangling_code_cpp(field_name, nodes_names, node_ref_name)
        else:
            raise Exception("unexpected destination language '{0}'".format(lang))

    def gen_mangling_code_cpp(self, field_name, nodes_names, node_ref_name):
        # FIXME
        raise NotImplementedError

    def gen_mangling_code_java(self, field_name, nodes_names, node_ref_name):
        if not self.visitable or self.ref_type not in nodes_names:
            return []

        getter_name = "get{0}".format(first_to_cap(field_name))

        if not self.optional:
            code = ["if ({0}.{1}() != null) {{".format(node_ref_name, getter_name)]
            node_expr = "{0}.{1}()".format(node_ref_name, getter_name)
        else:
            code = ["if ({0}.{1}() != null && {0}.{1}().isPresent()) {{".format(node_ref_name, getter_name)]
            node_expr = "{0}.{1}().get()".format(node_ref_name, getter_name)

        code.append(tab + "{0}.accept(this, null);".format(node_expr))
        code.append("}")

        return code

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

    def gen_subst_code(self, lang, field_name, nodes_names, indicators, manager_name):
        if lang == DST_LANGUAGE.CPP:
            return self.gen_subst_code_cpp(field_name, nodes_names, indicators, manager_name)
        elif lang == DST_LANGUAGE.JAVA:
            return self.gen_subst_code_java(field_name, nodes_names, indicators, manager_name)
        else:
            raise Exception("unexpected destination language '{0}'".format(lang))

    def gen_subst_code_cpp(self, field_name, nodes_names, indicators, manager_name):
        # FIXME
        raise NotImplementedError

    def gen_subst_code_java(self, field_name, nodes_names, indicators, manager_name):
        if not self.visitable:
            return []
        elif self.ref_type not in indicators and self.ref_type not in nodes_names:
            return []
        elif self.ref_type not in indicators and self.ref_type in nodes_names:
            if not self.optional:
                return [
                    "if (this.{0} != null) {{".format(field_name),
                    tab + "for ({0} node : this.{1}) {{".format(self.ref_type, field_name),
                    2 * tab + "node.substitute({0});".format(manager_name),
                    tab + "}",
                    "}"
                ]
            else:
                return [
                    "if (this.{0} != null && this.{0}.isPresent()) {{",
                    tab + "for ({0} node : this.{1}.get()) {{".format(self.ref_type, field_name),
                    2 * tab + "node.substitute({0});".format(manager_name),
                    tab + "}",
                    "}"
                ]

        indicator = indicators[self.ref_type]
        code = []

        if not self.optional:
            code.append("if (this.{0} != null) {{".format(field_name))
            it_expr = "this.{0}.listIterator()".format(field_name)
        else:
            code.append("if (this.{0} != null && this.{0}.isPresent()) {{".format(field_name))
            it_expr = "this.{0}.get().listIterator()".format(field_name)

        code.append(tab + "final ListIterator<{0}> it = {1};".format(self.ref_type, it_expr))
        code.append(tab + "while (it.hasNext()) {")
        code.append(2 * tab + "final {0} node = it.next();".format(self.ref_type))

        helper_code = self.gen_subst_code_helper("node", self.ref_type, indicator,
                                    manager_name, lambda s : "it.set({0});".format(s))
        helper_code = map(lambda s : 2 * tab + s, helper_code)
        code.extend(helper_code)

        code.append(tab + "}")
        code.append("}")

        return code

    def gen_set_paste_flag_deep(self, lang, field_name, nodes_names, value_param_name):
        if lang == DST_LANGUAGE.CPP:
            return self.gen_set_paste_flag_deep_cpp(field_name, nodes_names, value_param_name)
        elif lang == DST_LANGUAGE.JAVA:
            return self.gen_set_paste_flag_deep_java(field_name, nodes_names, value_param_name)
        else:
            raise Exception("unexpected destination language '{0}'".format(lang))

    def gen_set_paste_flag_deep_cpp(self, field_name, nodes_names, value_param_name):
        # FIXME
        raise NotImplementedError

    def gen_set_paste_flag_deep_java(self, field_name, nodes_names, value_param_name):
        if not self.visitable or self.ref_type not in nodes_names:
            return []

        if not self.optional:
            code = ["if (this.{0} != null) {{".format(field_name)]
            node_expr = "this.{0}".format(field_name)
        else:
            code = ["if (this.{0} != null && this.{0}.isPresent()) {{".format(field_name)]
            node_expr = "this.{0}.get()".format(field_name)

        code.append(tab + "for ({0} node : {1}) {{".format(self.ref_type, node_expr))
        code.append(2 * tab + "node.setPastedFlagDeep({0});".format(value_param_name))
        code.append(tab + "}")
        code.append("}")

        return code

    def gen_mangling_code(self, lang, field_name, nodes_names, node_ref_name):
        if lang == DST_LANGUAGE.JAVA:
            return self.gen_mangling_code_java(field_name, nodes_names, node_ref_name)
        elif lang == DST_LANGUAGE.CPP:
            return self.gen_mangling_code_cpp(field_name, nodes_names, node_ref_name)
        else:
            raise Exception("unexpected destination language '{0}'".format(lang))

    def gen_mangling_code_cpp(self, field_name, nodes_names, node_ref_name):
        # FIXME
        raise NotImplementedError

    def gen_mangling_code_java(self, field_name, nodes_names, node_ref_name):
        if not self.visitable or self.ref_type not in nodes_names:
            return []

        getter_name = "get{0}".format(first_to_cap(field_name))

        if not self.optional:
            code = ["if ({0}.{1}() != null) {{".format(node_ref_name, getter_name)]
            list_expr = "{0}.{1}()".format(node_ref_name, getter_name)
        else:
            code = ["if ({0}.{1}() != null && {0}.{1}().isPresent()) {{".format(node_ref_name, getter_name)]
            list_expr = "{0}.{1}().get()".format(node_ref_name, getter_name)

        code.append(tab + "for ({0} child : {1}) {{".format(self.ref_type, list_expr))
        code.append(2 * tab + "child.accept(this, null);")
        code.append(tab + "}")
        code.append("}")

        return code

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
