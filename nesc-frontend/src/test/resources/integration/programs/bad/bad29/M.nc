generic module M(typedef T)
{
   provides command void c(T arg);
}
implementation
{
   static T last_arg;

   command void c(T arg)
   {
      last_arg = arg;
   }
}
