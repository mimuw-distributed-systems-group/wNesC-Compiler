typedef int result_t;

interface Simple {

    command result_t send(int address, int length, void *msg);

    event result_t sendDone(void *msg, result_t success);

}
