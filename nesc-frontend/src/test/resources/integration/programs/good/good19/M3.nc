module M3
{
   provides command void c(const int* id);
   uses interface I<int, long>[int row, long int col];
   uses interface S<unsigned char>;
}
implementation
{
   static long long last_arg;

   event void I.e[int row, long col](int arg1, long arg2)
   {
      last_arg = arg1 + arg2;
   }

   command void c(const int* id)
   {
      last_arg = *id;
   }

   event void S.e(const unsigned char* ptr)
   {
      last_arg = *ptr;
   }
}
