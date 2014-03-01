module TokensLocations {
    uses interface Foo;
} implementation {
    char *broken_string = "This string is broken \
            with backslash \
            into multiline token";