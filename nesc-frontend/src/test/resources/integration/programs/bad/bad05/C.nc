/* A structure type referencing itself in a field. */

struct s
{
   long long int n;
   struct s nested;
};

configuration C
{
}
implementation
{
}
