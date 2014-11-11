module M2
{
   provides interface S<unsigned char>[unsigned char row, unsigned char col];
   uses event void overflow(void* ptr);
}
implementation
{
   static unsigned char store[256][256][1024];
   static int freeIndices[256][256];

   task void signalOverflow()
   {
      signal overflow(store);
   }

   command int S.c[unsigned char row, unsigned char col](unsigned char arg)
   {
      if (freeIndices[row][col] < 1024)
      {
         store[row][col][freeIndices[row][col]++] = arg;
         return 0;
      }
      else
      {
         post signalOverflow();
         return 1;
      }
   }
}
