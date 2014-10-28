/* Signaling bare non-parameterised events. */

configuration C
{
}
implementation
{
   components M1, M2;

   M2.b -> M1.b;
   M1.g -> M2.h;
}
