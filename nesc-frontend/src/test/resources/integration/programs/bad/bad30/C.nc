/* Invalid non-type argument for component (char[]). */

configuration C
{
}
implementation
{
   components new M(20), N;
   M.print -> N;
}
