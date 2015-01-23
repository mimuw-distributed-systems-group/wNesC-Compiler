from ast.util import DST_LANGUAGE, tab
import re


class DEEP_COPY_MODE:
    ASSIGN_NULL = 0
    ASSIGN_DEEP_COPY = 1
    ASSIGN_REFERENCE_COPY = 2
    ASSIGN_LIST_DEEP_COPY = 3
    ASSIGN_EXTERNAL_DEEP_COPY = 4


class FieldCopyCodeGenerator:
    """Base class for generators of code responsible for copying fields."""

    def __init__(self, field_name, field_optional, type_fun):
        self.field_name = field_name
        self.field_optional = field_optional
        self.type_fun = type_fun

    def generate_code(self, lang, copy_name, skip_param_name, nodes_map_param_name):
        """Returns a list with lines of code."""
        if lang == DST_LANGUAGE.JAVA:
            return self.generate_code_java(copy_name, skip_param_name, nodes_map_param_name)
        elif lang == DST_LANGUAGE.CPP:
            return self.generate_code_cpp(copy_name, skip_param_name, nodes_map_param_name)
        else:
            raise Exception("unexpected destination language {0}".format(lang))

    def generate_code_java(self, copy_name, skip_param_name, nodes_map_param_name):
        raise NotImplementedError

    def generate_code_cpp(self, copy_name, skip_param_name, nodes_map_param_name):
        raise NotImplementedError


class NullCopyCodeGenerator(FieldCopyCodeGenerator):
    def __init__(self, field_name, field_optional, type_fun):
        super().__init__(field_name, field_optional, type_fun)

    def generate_code_java(self, copy_name, skip_param_name, nodes_map_param_name):
        return ["{0}.{1} = null;".format(copy_name, self.field_name)]


class DeepCopyCodeGenerator(FieldCopyCodeGenerator):
    def __init__(self, field_name, field_optional, type_fun, external):
        self.external = external
        super().__init__(field_name, field_optional, type_fun)

    def generate_code_java(self, copy_name, skip_param_name, nodes_map_param_name):
        deep_copy_call = self.get_deep_copy_call(skip_param_name, nodes_map_param_name)

        if not self.field_optional:
            return [
                "{0}.{1} = this.{1} != null".format(copy_name, self.field_name),
                2 * tab + "? this.{0}.{1}".format(self.field_name, deep_copy_call),
                2 * tab + ": null;",
            ]
        else:
            # Retrieve the argument of the optional type
            field_type = self.type_fun(DST_LANGUAGE.JAVA)
            match = re.fullmatch(r'Optional<(?P<nested_type>.+)>', field_type)
            if match is None:
                raise Exception('unexpected type for an optional field {0}'.format(field_type))
            nested_type = match.group("nested_type")

            return [
                "if (this.{0} != null) {{".format(self.field_name),
                tab + "{0}.{1} = this.{1}.isPresent()".format(copy_name, self.field_name),
                3 * tab + "? Optional.of(this.{0}.get().{1})".format(self.field_name, deep_copy_call),
                3 * tab + ": Optional.<{0}>absent();".format(nested_type),
                "} else {",
                tab + "{0}.{1} = null;".format(copy_name, self.field_name),
                "}",
            ]

    def get_deep_copy_call(self, skip_param_name, nodes_map_param_name):
        return "deepCopy()" if self.external else "deepCopy({}, {})"\
            .format(skip_param_name, nodes_map_param_name)


class ReferenceCopyCodeGenerator(FieldCopyCodeGenerator):
    def __init__(self, field_name, field_optional, type_fun):
        super().__init__(field_name, field_optional, type_fun)

    def generate_code_java(self, copy_name, skip_param_name, nodes_map_param_name):
        return ["{0}.{1} = this.{1};".format(copy_name, self.field_name)]


class ListDeepCopyCodeGenerator(FieldCopyCodeGenerator):
    def __init__(self, field_name, field_optional, type_fun):
        super().__init__(field_name, field_optional, type_fun)

    def generate_code_java(self, copy_name, skip_param_name, nodes_map_param_name):
        return [
            "{0}.{1} = this.{1} != null".format(copy_name, self.field_name),
            2 * tab + "? AstUtils.deepCopyNodes(this.{0}, {1}, {2})"
                    .format(self.field_name, skip_param_name, nodes_map_param_name),
            2 * tab + ": null;",
        ]


def get_field_copy_gen(field_name, field):
    """Returns the generator of the copy code for given field."""
    mode = field.deep_copy_mode
    field_optional = field.optional
    type_fun = field.get_type

    if mode == DEEP_COPY_MODE.ASSIGN_NULL:
        return NullCopyCodeGenerator(field_name, field_optional, type_fun)
    elif mode == DEEP_COPY_MODE.ASSIGN_DEEP_COPY:
        return DeepCopyCodeGenerator(field_name, field_optional, type_fun, False)
    elif mode == DEEP_COPY_MODE.ASSIGN_REFERENCE_COPY:
        return ReferenceCopyCodeGenerator(field_name, field_optional, type_fun)
    elif mode == DEEP_COPY_MODE.ASSIGN_LIST_DEEP_COPY:
        return ListDeepCopyCodeGenerator(field_name, field_optional, type_fun)
    elif mode == DEEP_COPY_MODE.ASSIGN_EXTERNAL_DEEP_COPY:
        return DeepCopyCodeGenerator(field_name, field_optional, type_fun, True)
    else:
        raise Exception("invalid field deep copy mode {0}".format(mode))
