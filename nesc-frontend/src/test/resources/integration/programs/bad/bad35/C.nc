/* Jumping to inside of another atomic statement. */

void f()
{
   int n = 0;

   atomic
   {
   increment:
      if (n)
      {
         goto zero;
      }

      ++n;
      goto increment;
   }

   n = 1;

   atomic
   {
   zero:
      n = 0;
   }
}

configuration C
{
}
implementation
{
}
