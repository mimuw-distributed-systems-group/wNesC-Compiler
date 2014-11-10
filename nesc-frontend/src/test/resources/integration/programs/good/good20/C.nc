/* Correct instantiation of components. */

configuration C
{
   struct s { int n; char c; };
   enum e { CST1, CST2, CST3, };
}
implementation
{
   components N;
   components new M(char, float, struct s, 2, 2.0f, "M1") as M1;
   components new M(short, double, enum e, 10uLL, 2.0L, "M2") as M2;
   components new M(unsigned long long int, long double, float, 'a', 10.0, "M3") as M3;
   components new M(unsigned, short, void*, 22L, 9.11, "M4") as M4;
   components new M(unsigned char, unsigned char, struct s*, 123uL, 10.11F, "M5") as M5;
   components new M(long long int, unsigned long long, void**, 98LL, 11.11l, "M6") as M6;
}
