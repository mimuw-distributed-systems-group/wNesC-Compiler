/* Aliasing an incomplete type. */

typedef struct s alias_s;

void f()
{
   struct s
   {
      unsigned char byte;
      signed short int num;
   };

   alias_s n;
}

configuration C
{
}
implementation
{
}
