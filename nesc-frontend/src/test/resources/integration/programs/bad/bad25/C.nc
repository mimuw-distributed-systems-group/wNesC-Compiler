/* Invalid implicit link wires (no proper specification entity in component). */

configuration C
{
}
implementation
{
   components M1, M2;
   M1.c -> M2;
}
