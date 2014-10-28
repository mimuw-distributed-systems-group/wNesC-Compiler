module M
{
   uses interface I<unsigned long long>;
}
implementation
{
   event void I.e(unsigned long long i)
   {
      call I.c(20uLL + i, "a");
   }
}
