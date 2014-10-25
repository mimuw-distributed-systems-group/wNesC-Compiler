module M
{
   provides interface I<float>;
}
implementation
{
   command float I.get()
   {
      return 7.0f;
   }
}
