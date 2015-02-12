package pl.edu.mimuw.nesc.constexpr.value;

import org.junit.Test;
import static pl.edu.mimuw.nesc.constexpr.value.ConstantValueAssert.*;

/**
 * <p>The tested counts of bits are the same as in
 * {@link SignedIntegerConstantValueTest}.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class UnsignedIntegerConstantValueTest {
    private static final String ZERO = "0";

    @Test
    public void testExactAddPositiveNumbers() {
        assertUnsignedIntAdd("1", "1", "2", 3);
        assertUnsignedIntAdd("1", "1", "2", 8);
        assertUnsignedIntAdd("1", "1", "2", 10);
        assertUnsignedIntAdd("1", "1", "2", 16);
        assertUnsignedIntAdd("1", "1", "2", 23);
        assertUnsignedIntAdd("1", "1", "2", 32);
        assertUnsignedIntAdd("1", "1", "2", 61);
        assertUnsignedIntAdd("1", "1", "2", 64);
        assertUnsignedIntAdd("1", "1", "2", 100);
        assertUnsignedIntAdd("1", "1", "2", 129);
        assertUnsignedIntAdd("1", "1", "2", 302);

        assertUnsignedIntAdd("1", "6", "7", 3);
        assertUnsignedIntAdd("69", "186", "255", 8);
        assertUnsignedIntAdd("602", "421", "1023", 10);
        assertUnsignedIntAdd("14804", "50731", "65535", 16);
        assertUnsignedIntAdd("4752882", "3635725", "8388607", 23);
        assertUnsignedIntAdd("4026104557", "268862738", "4294967295", 32);
        assertUnsignedIntAdd("2268123114957375392", "37719894256318559", "2305843009213693951", 61);
        assertUnsignedIntAdd("5887324053282691405", "12559420020426860210", "18446744073709551615", 64);
        assertUnsignedIntAdd("177857238519485674569227206085", "1089793361708743726927475999290",
                "1267650600228229401496703205375", 100);
        assertUnsignedIntAdd("671601418613733139692955570321089516531", "8963315228143787233793644542446906380",
                "680564733841876926926749214863536422911", 129);
        assertUnsignedIntAdd("4816980215675888100583420277652692414018624899049253835457319548648159665021476763033895823",
                "3331163689662056244490362475984820230187248675614491167087242248769365534031870061699693680",
                "8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503", 302);
    }

    @Test
    public void testExactAddZero() {
        assertUnsignedIntAdd("1", ZERO, "1", 3);
        assertUnsignedIntAdd(ZERO, "1", "1", 8);
        assertUnsignedIntAdd(ZERO, "1", "1", 10);
        assertUnsignedIntAdd("1", ZERO, "1", 16);
        assertUnsignedIntAdd(ZERO, "1", "1", 23);
        assertUnsignedIntAdd(ZERO, "1", "1", 32);
        assertUnsignedIntAdd("1", ZERO, "1", 61);
        assertUnsignedIntAdd("1", ZERO, "1", 64);
        assertUnsignedIntAdd("1", ZERO, "1", 100);
        assertUnsignedIntAdd("1", ZERO, "1", 129);
        assertUnsignedIntAdd("1", ZERO, "1", 302);

        assertUnsignedIntAdd(ZERO, "7", "7", 3);
        assertUnsignedIntAdd("7", ZERO, "7", 3);
        assertUnsignedIntAdd(ZERO, "255", "255", 8);
        assertUnsignedIntAdd("255", ZERO, "255", 8);
        assertUnsignedIntAdd(ZERO, "1023", "1023", 10);
        assertUnsignedIntAdd("1023", ZERO, "1023", 10);
        assertUnsignedIntAdd(ZERO, "65535", "65535", 16);
        assertUnsignedIntAdd("65535", ZERO, "65535", 16);
        assertUnsignedIntAdd(ZERO, "8388607", "8388607", 23);
        assertUnsignedIntAdd("8388607", ZERO, "8388607", 23);
        assertUnsignedIntAdd(ZERO, "4294967295", "4294967295", 32);
        assertUnsignedIntAdd("4294967295", ZERO, "4294967295", 32);
        assertUnsignedIntAdd(ZERO, "2305843009213693951", "2305843009213693951", 61);
        assertUnsignedIntAdd("2305843009213693951", ZERO, "2305843009213693951", 61);
        assertUnsignedIntAdd(ZERO, "18446744073709551615", "18446744073709551615", 64);
        assertUnsignedIntAdd("18446744073709551615", ZERO, "18446744073709551615", 64);
        assertUnsignedIntAdd(ZERO, "1267650600228229401496703205375", "1267650600228229401496703205375", 100);
        assertUnsignedIntAdd("1267650600228229401496703205375", ZERO, "1267650600228229401496703205375", 100);
        assertUnsignedIntAdd(ZERO, "680564733841876926926749214863536422911",
                "680564733841876926926749214863536422911", 129);
        assertUnsignedIntAdd("680564733841876926926749214863536422911", ZERO,
                "680564733841876926926749214863536422911", 129);
        assertUnsignedIntAdd(ZERO, "8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503",
                "8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503",
                302);
        assertUnsignedIntAdd("8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503",
                ZERO, "8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503",
                302);
    }

    @Test
    public void testOverflowAdd() {
        assertUnsignedIntAdd("1", "7", "0", 3);
        assertUnsignedIntAdd("7", "1", "0", 3);
        assertUnsignedIntAdd("1", "255", "0", 8);
        assertUnsignedIntAdd("255", "1", "0", 8);
        assertUnsignedIntAdd("1", "1023", "0", 10);
        assertUnsignedIntAdd("1023", "1", "0", 10);
        assertUnsignedIntAdd("1", "65535", "0", 16);
        assertUnsignedIntAdd("65535", "1", "0", 16);
        assertUnsignedIntAdd("1", "8388607", "0", 23);
        assertUnsignedIntAdd("8388607", "1", "0", 23);
        assertUnsignedIntAdd("1", "4294967295", "0", 32);
        assertUnsignedIntAdd("4294967295", "1", "0", 32);
        assertUnsignedIntAdd("1", "2305843009213693951", "0", 61);
        assertUnsignedIntAdd("2305843009213693951", "1", "0", 61);
        assertUnsignedIntAdd("1", "18446744073709551615", "0", 64);
        assertUnsignedIntAdd("18446744073709551615", "1", "0", 64);
        assertUnsignedIntAdd("1", "1267650600228229401496703205375", "0", 100);
        assertUnsignedIntAdd("1267650600228229401496703205375", "1", "0", 100);
        assertUnsignedIntAdd("1", "680564733841876926926749214863536422911", "0", 129);
        assertUnsignedIntAdd("680564733841876926926749214863536422911", "1", "0", 129);
        assertUnsignedIntAdd("1", "8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503", "0", 302);
        assertUnsignedIntAdd("8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503", "1", "0", 302);

        assertUnsignedIntAdd("7", "7", "6", 3);
        assertUnsignedIntAdd("255", "255", "254", 8);
        assertUnsignedIntAdd("1023", "1023", "1022", 10);
        assertUnsignedIntAdd("65535", "65535", "65534", 16);
        assertUnsignedIntAdd("8388607", "8388607", "8388606", 23);
        assertUnsignedIntAdd("4294967295", "4294967295", "4294967294", 32);
        assertUnsignedIntAdd("2305843009213693951", "2305843009213693951", "2305843009213693950", 61);
        assertUnsignedIntAdd("18446744073709551615", "18446744073709551615", "18446744073709551614", 64);
        assertUnsignedIntAdd("1267650600228229401496703205375", "1267650600228229401496703205375",
                "1267650600228229401496703205374", 100);
        assertUnsignedIntAdd("680564733841876926926749214863536422911", "680564733841876926926749214863536422911",
                "680564733841876926926749214863536422910", 129);
        assertUnsignedIntAdd("8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503",
                "8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503",
                "8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589502", 302);
    }
}
