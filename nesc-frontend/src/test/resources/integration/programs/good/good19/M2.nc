generic module M2(typedef T @integer())
{
   provides command void perform(const int* id);
   uses interface I<int, T> as IMatrix[int row, long int col];
   uses interface S<unsigned char>;
}
implementation
{
   static T last_arg;
   static int last_row;
   static long int last_col;

   static int free;
   static long long int arg_copy;

   task void next()
   {
      call IMatrix.c[last_row, last_col](last_arg, last_arg - last_row);

      if (free)
      {
         free = 0;
         arg_copy = last_arg;
         call S.c((unsigned char*) &arg_copy);
      }
   }

   event void IMatrix.e[int row, long col](int arg1, T arg2)
   {
      last_arg = arg1 + arg2;
      last_row = row;
      last_col = col;
   }

   command void perform(const int* id)
   {
      last_arg = *id;
      post next();
   }

   event void S.e(const unsigned char* id)
   {
      free = 1;
   }
}
