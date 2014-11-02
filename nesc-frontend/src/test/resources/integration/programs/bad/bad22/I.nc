interface I<T @integer()>
{
   command T c();
   event void e(T id);
}
