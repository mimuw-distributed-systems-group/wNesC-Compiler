generic module M(typedef T)
{
   struct node
   {
      T val;
      struct node* parent;
   };

   provides command void f(T t);
}
implementation
{
   command void f(T t)
   {
      struct node node;
      const T* ptr_t;

      ptr_t = &t;
      node.val = *ptr_t;
   }
}
