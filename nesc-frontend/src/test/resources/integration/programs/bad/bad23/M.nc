module M
{
   provides event void createDone(void* newObject);

   uses interface I;
   uses command void create(int arg);
}
implementation
{
   event void I.e()
   {
      call create(7);
   } 
}
