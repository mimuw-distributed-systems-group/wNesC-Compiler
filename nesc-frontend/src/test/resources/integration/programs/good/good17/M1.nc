module M1
{
   provides event void b();
   uses event void g(void* ptr);
}
implementation
{
   static int n;

   event void b()
   {
      n = 10;
      signal g(&n);
   }
}
