generic module M(typedef T)
{
   provides interface I<T> as PI;
   uses interface I<T> as UI;
}
implementation
{
   static T a;

   command void PI.c(T arg)
   {
      a = arg;
   }

   event void UI.e(T arg)
   {
      a = arg;
   }
}
