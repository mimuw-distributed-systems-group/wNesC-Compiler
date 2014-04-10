#define STRINGIZE(x) #x
#define STR(x) STRINGIZE(x) "!"
#define APPEND_SEMICOLON(x) x ";"
#define COPY(x) x
#define ONE 1

STRINGIZE(ONE);
STRINGIZE();
COPY(APPEND_SEMICOLON(STRINGIZE(int x)));
STR(ONE);
STR("abc");