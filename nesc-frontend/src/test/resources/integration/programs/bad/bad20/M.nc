module M
{
   provides event void e();
   uses interface I[unsigned char i];
}
implementation
{
   event void e()
   {
      // interface I doesn't contain command or event 'h'
      call I.h[0]();
   }

   event void I.e[unsigned char i]()
   {
      return;
   }
}
