generic module M(char instantiation_id[])
{
   uses command void print(const char* str);
   provides command void c();
}
implementation
{
   command void c()
   {
      call print(instantiation_id);
   }
}
