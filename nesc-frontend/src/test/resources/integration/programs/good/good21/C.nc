/* All external specification elements correctly wired. */

configuration C
{
   enum e { CST1, CST2, };

   provides
   {
      interface S<int>;
      interface S<enum e> as S2;
      command void pc(long arg1, unsigned arg2, enum e arg3);
      event enum e* pe();
      command int last();
      interface S<unsigned char> as S6[unsigned char id];
      interface S<unsigned char> as S7[unsigned char row, unsigned char col];
   }

   uses
   {
      interface S<int> as S3;
      interface S<enum e> as S4;
      interface S<unsigned char> as S5[unsigned char id];
      command void uc(long arg1, unsigned arg2, enum e arg3);
      event enum e* ue();
      event void e(void* memptr);
   }
}
implementation
{
   /* Fulfill wiring by equating an external specification element with an
      element from another component. */
   components new M1(int) as Mint;
   S = Mint;
   S3 = Mint;
   Mint.getLast = last;

   components M2;
   M2 = e;
   S7 = M2;

   // Fulfill wiring by equating two external specification elements
   pc = uc;
   pe = ue;
   S2 = S4;
   S5 = S6;
}
