generic module M(typedef @number() T)
{
   provides command void c(T n);
}
implementation
{
   command void c(T n)
   {
      int i;
      float x;

      i = n;
      x = n;
      i += x * n + 10;
      x /= n + 7;

      /* Each of these two statements produces the following error:
      
            "conversion to non-scalar type requested"
         
         in ncc compiler. However, they are correct according to the NesC
         reference. 'n' variable is of type 'T' which has @number() attribute.
         It means that T must be an integer or floating type so also an
         arithmetic type and consequently a scalar type. It implies that 'n'
         has arithmetic type. Moreover, a value of an arithmetic type can be
         added or assigned to a modifiable lvalue of an arithmetic type. 'n' is
         a modifiable lvalue. */
      n += i;
      n = n - x;
   }
}
