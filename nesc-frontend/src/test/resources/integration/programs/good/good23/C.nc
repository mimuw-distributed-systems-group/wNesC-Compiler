/* Correct usage of 'goto' with atomic statements. */

int n;

int f()
{
   if (!n)
   {
      goto failure;
   }

   atomic
   {
      n = 0;
   repeat:
      ++n;

      if (n != 10) {
         goto repeat;
      }
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
