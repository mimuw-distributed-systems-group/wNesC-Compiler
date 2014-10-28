module M2
{
   uses interface I<short, double>;
   provides command void c();
}
implementation
{
   static short value;
   static short snd_value;

   command void c()
   {
      snd_value = 2;
      value = call I.c(&snd_value);
   }

   event void I.e(const volatile short* ptr)
   {
      value += *ptr;
   }
}
