module M
{
   provides command void run();
}
implementation
{
   static int n;
   task void another_task();

   task void simple_task()
   {
      post another_task();
   }

   task void another_task()
   {
      // no data races because of tasks atomicity
      n += post another_task();
   }

   command void run()
   {
      n = 0;
      post simple_task();
   }
}
