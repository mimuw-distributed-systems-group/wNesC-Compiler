/* Invalid type argument for a component (function type). */

configuration C
{
}
implementation
{
   components new M(void(int, long int));
}
