interface I<T>
{
   command T c(T* ptr);
   event void e(int code, T* ptr);
}
