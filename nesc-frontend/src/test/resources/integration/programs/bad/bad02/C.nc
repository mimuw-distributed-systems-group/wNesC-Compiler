/* Nested tag redefinition. */

struct s
{
   int i;

   struct s
   {
      char c;
   };
};

configuration C
{
}
implementation
{
}
