module M1
{
   provides interface I<short, double>[unsigned int i];
}
implementation
{
   static const short* short_ptr;
   static unsigned int id;

   task void doSignal()
   {
      signal I.e[id](short_ptr); 
   }

   command double I.c[unsigned int i](const short* s)
   {
      short_ptr = s + i;
      id = i;

      return post doSignal();
   }
}
