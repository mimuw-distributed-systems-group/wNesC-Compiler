/* Reference to component's fields (only enums). */

configuration C
{
}
implementation
{
   components M;
   enum { CONST3 = M.CONST1 + M.CONST2, };
}
