/* Redefinition of a tag. */

configuration C
{
   struct s
   {
      int i;
      char* buffer;
   };

   struct s
   {
      const char* name;
      float result;
   };
}
implementation
{
}
