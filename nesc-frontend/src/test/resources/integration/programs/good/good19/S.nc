interface S<T>
{
   command void c(const T* ptr);
   event void e(const T* ptr);
}
