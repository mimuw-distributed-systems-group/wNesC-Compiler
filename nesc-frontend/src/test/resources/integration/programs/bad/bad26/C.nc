/* Connecting two provided external specification elements. */

configuration C
{
   provides command void c1();
   provides command void c2();
}
implementation
{
   c1 = c2;
}
