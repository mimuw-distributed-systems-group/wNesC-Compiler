module M
{
   provides interface I<unsigned char>;
   provides event void e();
   uses command void c();
}
implementation
{
   static int acc;

   event void e()
   {
      acc = 0;
   }
}
