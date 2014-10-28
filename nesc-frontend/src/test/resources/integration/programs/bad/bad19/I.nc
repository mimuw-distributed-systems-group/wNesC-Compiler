interface I<T @integer()>
{
   command void c(T t);
   event void e(T t);
}
