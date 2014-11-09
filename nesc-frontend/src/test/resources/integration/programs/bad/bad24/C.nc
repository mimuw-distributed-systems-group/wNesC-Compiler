/* Invalid explicit link wires (wrong generic parameters of an interface). */

configuration C
{
}
implementation
{
   components new M(int*) as M1, new M(long*) as M2;
   M1.UI -> M2.PI;
}
