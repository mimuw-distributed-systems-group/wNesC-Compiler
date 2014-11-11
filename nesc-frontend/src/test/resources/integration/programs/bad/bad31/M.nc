module M
{
   uses interface I<short>[unsigned char clientId];
}
implementation
{
   static short last;

   event void I.e[unsigned char clientId](short arg)
   {
      call I.c[clientId](last);
      last = arg;
   }
}
