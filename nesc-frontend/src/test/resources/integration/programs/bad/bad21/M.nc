module M
{
   provides command void c();
   uses event void e();
}
implementation
{
   task void t()
   {
      // bare event 'd' isn't declared in the specification
      signal d();
   }

   command void c()
   {
      post t();
   }
}
