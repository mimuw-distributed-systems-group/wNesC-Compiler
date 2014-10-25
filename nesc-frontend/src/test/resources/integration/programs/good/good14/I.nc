interface I<T1, T2 @number(), T3 @integer()>
{
   command T3 perform(T1 t1, T2 t2);
}
