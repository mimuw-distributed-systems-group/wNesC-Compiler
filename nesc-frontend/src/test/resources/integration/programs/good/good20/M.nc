generic module M(
   typedef TI @integer(),
   typedef @number() TN,
   typedef T,
   short int short_cst,
   float floating_cst,
   char str_cst[]
)
{
   provides command void c(TI n, TN x, T u);
}
implementation
{
   static T last_u;
   static long long int sum;

   command void c(TI n, TN x, T u)
   {
      last_u = u;
      sum += n + x;
      sum += short_cst;
      sum += floating_cst;
   }
}
