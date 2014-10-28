/* Signaling an event from a parameterised interface (and calling a command
   from a non-parameterised interface). */

configuration C
{
}
implementation
{
   components M1, M2;
   M2.I -> M1.I[unique("M1.I")];
}
