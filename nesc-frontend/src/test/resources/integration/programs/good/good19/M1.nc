generic module M1(typedef T @integer())
{
   provides interface I<T, long int>[int row, long int col];
   uses command void make(const T* id);
}
implementation
{
   static unsigned long int abs_sum;
   static T last_row;

   command void I.c[int row, long int col](T arg1, long int arg2)
   {
      abs_sum += arg1 < 0 ? -arg1 : arg1;
      abs_sum += arg2 < 0 ? -arg2 : arg2;
      last_row = row;
      call make(&last_row);
   }
}
