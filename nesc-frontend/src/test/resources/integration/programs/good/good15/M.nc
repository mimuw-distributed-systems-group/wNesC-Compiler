module M
{
   provides interface I<long>;
}
implementation
{
   task void doSignal()
   {
      signal I.initDone(5);
   }

   command void I.init()
   {
      post doSignal();
   }
}
