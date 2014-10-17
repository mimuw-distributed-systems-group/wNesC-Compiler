/* Invalid usage of a variable of a type with @integer() attribute. */

configuration C
{
}
implementation
{
   components new M(unsigned long);
}
