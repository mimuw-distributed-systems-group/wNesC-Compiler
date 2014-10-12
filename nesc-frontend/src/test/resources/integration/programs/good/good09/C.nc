/* Reference to component's fields (only typedef). */

configuration C
{
}
implementation
{
   components M;
   typedef M.custom_t my_t;
}
