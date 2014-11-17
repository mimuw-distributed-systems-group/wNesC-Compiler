from collections import OrderedDict


#a small utility function for capitalizing class names
def first_to_cap(string):
    return string[0].capitalize() + string[1:]


#since python lacks enums we use classes instead
#DST_LANGUAGE represents the target language for the generation of code
class DST_LANGUAGE:
    CPP = 0
    JAVA = 1

#defines the width of the indentation in the generated code
tab = " " * 4

#A dictionary of all nodes of the abstract syntax tree
ast_nodes = OrderedDict()
ast_enums = OrderedDict()

#Dictionary with nodes that have an activated generic indicator
generic_nodes = {}
