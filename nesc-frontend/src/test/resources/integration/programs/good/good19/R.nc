module R
{
   provides interface S<unsigned char>[unsigned char client_id];
}
implementation
{
   static unsigned char c;
   static unsigned char last_id;
   static const unsigned char* last_ptr;

   task void doSignal()
   {
      signal S.e[last_id](last_ptr);
   }

   command void S.c[unsigned char client_id](const unsigned char* value)
   {
      c = *value; 
      last_id = client_id;
      last_ptr = value;
      post doSignal();
   }
}
