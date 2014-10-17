generic module M(typedef T)
{
   provides command void c(T t);
}
implementation
{
   command void c(T t)
   {
      int i;
      i = t;
   }
}
