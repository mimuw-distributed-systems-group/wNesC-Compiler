/* Reference to an unplaced label. */

int n;

int f()
{
   if (!n)
   {
      goto fail;
   }

   return 0;

failure:
   return 1;
}

configuration C
{
}
implementation
{
}
