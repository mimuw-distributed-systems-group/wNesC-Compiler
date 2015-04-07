from collections import OrderedDict


#a small utility function for capitalizing class names
def first_to_cap(string):
    return string[0].capitalize() + string[1:]


#since python lacks enums we use classes instead
#DST_LANGUAGE represents the target language for the generation of code
class DST_LANGUAGE:
    CPP = 0
    JAVA = 1


def language_dispatch(lang, java_fun, cpp_fun, *args, **kwargs):
    if lang == DST_LANGUAGE.JAVA:
        return java_fun(*args, **kwargs)
    elif lang == DST_LANGUAGE.CPP:
        return cpp_fun(*args, **kwargs)
    else:
        raise Exception("unexpected destination language '{0}'".format(lang))


def is_subnode(classname, maybe_ancestor):
    if classname not in ast_nodes:
        return classname == maybe_ancestor

    cls = ast_nodes[classname]

    while hasattr(cls, "superclass"):
        if cls.__name__ == maybe_ancestor:
            return True
        cls = cls.superclass

    return cls.__name__ == maybe_ancestor


#defines the width of the indentation in the generated code
tab = " " * 4

#A dictionary of all nodes of the abstract syntax tree
ast_nodes = OrderedDict()
ast_enums = OrderedDict()

#Dictionary with nodes that have an activated generic indicator
generic_nodes = {}

#Dictionary with nodes that have a mangle indicator activated
mangle_nodes = {}

#Dictionary with nodes that have a unique indicator
unique_nodes = {}
