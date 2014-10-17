/* Operations with a type parameter in a generic module. */

configuration C
{
}
implementation
{
   typedef struct intlist
   {
      int value;
      struct intlist* next; 
   } intlist;

   components new M(intlist);
}
