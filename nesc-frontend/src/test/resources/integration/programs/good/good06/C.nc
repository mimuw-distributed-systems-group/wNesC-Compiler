/* Aliasing a type completed later. */

typedef struct s alias_s;

struct s
{
   double r;
   unsigned long n;
};

void f()
{
   alias_s s;
}

configuration C
{
}
implementation
{
}
