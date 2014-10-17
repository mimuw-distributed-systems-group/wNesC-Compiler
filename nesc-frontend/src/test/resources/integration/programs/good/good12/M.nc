generic module M(typedef T @integer())
{
   provides command void c(T t);
}
implementation
{
   command void c(T t)
   {
      int* ptr_i;
      float n;

      ptr_i = (int*) t;
      n = (long) n % (long) t;
      t ^= sizeof t;
      t = t << sizeof(T);
   }
}
