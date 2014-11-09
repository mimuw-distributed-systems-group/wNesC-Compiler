/* Different kinds of wiring. */

configuration C
{
   provides
   {
      interface I<int, long>[int row, long int col];
      interface S<unsigned char>;
      interface S<unsigned char> as S3[unsigned char id];
      interface S<unsigned char> as S4[unsigned int i];
   }

   uses
   {
      interface S<unsigned char> as S2[unsigned int i];
      command void c(const int* id);
   }
}
implementation
{
   components new M1(int), new M2(long int), R;

   // Explicit link wires
   M1.I <- M2.IMatrix;
   M1.make -> M2.perform;
   M2.S -> R.S[unique("R")];

   // Implicit link wires
   components M3;
   M3.I -> M1;
   M3.c <- M1;
   M3 -> R.S[unique("R")];

   // Explicit equate wires
   S3 = R.S;
   S = S2[0];
   S = R.S[unique("R")];
   S2 = S4;
   
   // Implicit equate wires
   M1 = I;
   c = M1;
   S2[1] = M3;
}
