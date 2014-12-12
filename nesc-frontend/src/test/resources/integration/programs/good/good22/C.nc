/* Assignment of a pointer to a function. */

int f()
{
   return 7;
}

int g()
{
   int (*ptr_f)();
   ptr_f = f;
   return ptr_f();
}

configuration C
{
}
implementation
{
}
