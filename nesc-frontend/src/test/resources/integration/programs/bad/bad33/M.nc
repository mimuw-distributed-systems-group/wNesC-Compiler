module M
{
   provides command void c();
}
implementation
{
   task void t();

   command void c()
   {
       post t();
   }
}
