/* Bare command not wired. */

configuration C
{
   provides command int c(void* ptr);
   uses interface I<short>;
}
implementation
{
   components M;
   M.I[unique("M")] = I;
}

