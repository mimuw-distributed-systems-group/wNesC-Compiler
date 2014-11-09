module M1
{
   uses command void c();
   provides event void e();
}
implementation
{
   event void e()
   {
      call c();
   }
}
