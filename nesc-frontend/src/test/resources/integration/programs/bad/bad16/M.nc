module M
{
   provides command void c();
}
implementation
{
   static int n;

   static void f()
   {
      ++n;
   }

   command void c()
   {
      n = 0;
      post f();
   }
}
