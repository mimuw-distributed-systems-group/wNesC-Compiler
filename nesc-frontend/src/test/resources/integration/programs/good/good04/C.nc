/* Shadowing tags with the same names. */

struct tag
{
   unsigned long long int n;
   signed char c;
};

configuration C
{
   typedef struct tag struct_tag;

   union tag
   {
      int n;
      float f;
   };
}
implementation
{
   typedef union tag union_tag;

   struct tag
   {
      union_tag u;
      struct_tag s;
   };
}
