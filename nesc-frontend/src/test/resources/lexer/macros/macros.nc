#define ADD2(x, y) x + y
#define ADD3(x, y, z) ADD2(x, y) + z
#define SUB(x, y) x - y
#define ZERO 0
#define STR "s"
#define EMPTY

ADD2(ZERO, 1);
ADD3(ZERO, 1, 2);
ADD2(1, SUB(ZERO, 1));
ADD2(1, ADD2(ZERO, 1));
STR;
EMPTY
