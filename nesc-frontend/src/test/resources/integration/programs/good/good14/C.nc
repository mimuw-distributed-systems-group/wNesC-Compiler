/* Valid interface reference parameters. */

configuration C
{
   provides interface I<char*, long double, long int>;
}
implementation
{
   components M;
   I = M.I;
}
