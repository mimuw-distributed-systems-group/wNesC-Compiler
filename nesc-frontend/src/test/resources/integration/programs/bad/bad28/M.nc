generic module M(typedef T, int size)
{
   provides command void c();
}
implementation
{
   static int counter;

   command void c()
   {
      ++counter;
   }
}
