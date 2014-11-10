module N
{
   provides command void c(char arg);
}
implementation
{
   static char sum;

   command void c(char arg)
   {
      sum += arg;
   }
}
