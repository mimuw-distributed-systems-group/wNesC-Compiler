/* Invalid implicit equate wires (too many matches). */

configuration C
{
   provides interface I<short>[char dim1, short dim2, long dim3];
}
implementation
{
   components M;
   I = M;
}
