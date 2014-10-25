module M
{
   provides interface I<char*, long double, long>;
}
implementation
{
   command long I.perform(char* buf, long double v)
   {
      return *((long*) buf) + (long) v;
   }
}
