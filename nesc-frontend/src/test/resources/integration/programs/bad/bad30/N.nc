module N
{
   provides command void print(const char* str);
}
implementation
{
   char* buf;

   command void print(const char* str)
   {
      int i = 0;

      while (str[i] != '\0')
      {
         *buf = str[i];
      }
   }
}
