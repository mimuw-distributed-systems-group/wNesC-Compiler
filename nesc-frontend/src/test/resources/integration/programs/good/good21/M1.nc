generic module M1(typedef T)
{
   provides interface S<T> as ProvidedS;
   uses interface S<T> as UsedS;
   provides command T getLast();
}
implementation
{
   static T lastElement;

   command int ProvidedS.c(T t)
   {
      lastElement = t;
      return call UsedS.c(t);
   }

   event void UsedS.e(T t)
   {
      signal ProvidedS.e(t);
   }

   command T getLast()
   {
      return lastElement;
   }
}
