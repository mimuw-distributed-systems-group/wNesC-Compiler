generic module M(typedef T @number())
{
   provides command void c(T t);
}
implementation
{
   command void c(T t)
   {
      int* ptr_i;
      ptr_i = (int*) t; 
   }
}
