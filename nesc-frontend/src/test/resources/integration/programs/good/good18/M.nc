module M
{
   enum e { CST1, CST2, };

   provides
   {
      interface I<int>;
      interface I<long> as I2[unsigned char id];
      command void pc(enum e value);
      event void* pe();
   }

   uses
   {
      interface I<enum e*> as I3[unsigned long long large_id];
      event void ue(enum e* value);
      command void uc();
   }
}
implementation
{
   command int I.c(int* ptr)
   {
      return *ptr;
   }

   default event void I.e(int code, int* ptr)
   {
      code = *ptr;
   }

   command long I2.c[unsigned char id](long* ptr)
   {
      return *ptr + id;
   }

   default event void I2.e[unsigned char id](int code, long* ptr)
   {
      *ptr += code;
      *ptr += id;
   }

   command void pc(enum e value)
   {
      enum e* ptr;
      ptr = &value;

      switch (value)
      {
         case CST1:
            call I3.c[0uLL](&ptr);
            break;
         case CST2:
            call uc();
            signal I.e(2, 0);
            break;
         default:
            break;
      }
   }

   event void* pe()
   {
      return 0;
   }

   default command enum e* I3.c[unsigned long long id](enum e** ptr)
   {
      return *ptr + id;
   }

   event void I3.e[unsigned long long id](int code, enum e** ptr)
   {
      code = (int) ptr;
   }

   default event void ue(enum e* value)
   {
      *value = CST2;
   }

   default command void uc()
   {
      return;
   }
}
