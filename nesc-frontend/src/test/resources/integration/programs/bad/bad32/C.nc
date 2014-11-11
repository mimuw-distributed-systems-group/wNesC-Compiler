/* Interface not wired. */

configuration C
{
   uses interface I<char>[long long id];
   provides interface I<unsigned> as I2;
   uses interface I<unsigned int> as I3;
}
implementation
{
   I2 = I3; 
}
