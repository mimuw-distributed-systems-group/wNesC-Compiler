interface I<T1 @integer(), T2 @number()>
{
   command T2 c(const T1* ptr);
   event void e(const volatile T1* ptr);
}
