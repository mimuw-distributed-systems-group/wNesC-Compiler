generic module M(typedef @integer() T)
{
   provides command void c(T t);
}
implementation
{
   command void c(T t)
   {
      float* ptr_f;
      struct { int n; char c; } s;

      ptr_f = (float*) t;
      *ptr_f = ptr_f ? s : t; 
   }
}
