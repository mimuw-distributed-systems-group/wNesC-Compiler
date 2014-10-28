module M2
{
   provides event void h(void* ptr);
   uses event void b();
}
implementation
{
   static char c;

   task void doSignal()
   {
      signal b();
   }

   @hwevent()
   void interrupt()
   {
      post doSignal();
   }

   event void h(void* ptr)
   {
      c = * (char*) ptr;      
   }
}
