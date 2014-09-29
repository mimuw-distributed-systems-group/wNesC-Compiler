/* Nested definition with forward declaration of a tag */

struct s;

struct s
{
   signed char c;

   struct s
   {
      signed long long int r;
   };
};

configuration C
{
}
implementation
{
}
