module M
{
   provides interface I<short> as I1[char dim1, short dim2, long dim3];
   provides interface I<short> as I2[char dim1, short dim2, long dim3];
   provides interface I<short> as I3[char dim1, short dim2, long dim3];
}
implementation
{
   long long acc;

   command void I1.c[char dim1, short dim2, long dim3](short arg)
   {
      acc = dim1 + dim2 + dim3 + arg;
   }

   command void I2.c[char dim1, short dim2, long dim3](short arg)
   {
      acc = dim1 + dim2 + dim3 + arg;
   }

   command void I3.c[char dim1, short dim2, long dim3](short arg)
   {
      acc = dim1 + dim2 + dim3 + arg;
   }
}
