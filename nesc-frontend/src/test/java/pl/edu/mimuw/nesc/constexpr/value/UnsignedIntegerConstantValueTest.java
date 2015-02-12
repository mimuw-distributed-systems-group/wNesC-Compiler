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
        assertUnsignedIntAdd("1", "7", ZERO, 3);
        assertUnsignedIntAdd("7", "1", ZERO, 3);
        assertUnsignedIntAdd("1", "255", ZERO, 8);
        assertUnsignedIntAdd("255", "1", ZERO, 8);
        assertUnsignedIntAdd("1", "1023", ZERO, 10);
        assertUnsignedIntAdd("1023", "1", ZERO, 10);
        assertUnsignedIntAdd("1", "65535", ZERO, 16);
        assertUnsignedIntAdd("65535", "1", ZERO, 16);
        assertUnsignedIntAdd("1", "8388607", ZERO, 23);
        assertUnsignedIntAdd("8388607", "1", ZERO, 23);
        assertUnsignedIntAdd("1", "4294967295", ZERO, 32);
        assertUnsignedIntAdd("4294967295", "1", ZERO, 32);
        assertUnsignedIntAdd("1", "2305843009213693951", ZERO, 61);
        assertUnsignedIntAdd("2305843009213693951", "1", ZERO, 61);
        assertUnsignedIntAdd("1", "18446744073709551615", ZERO, 64);
        assertUnsignedIntAdd("18446744073709551615", "1", ZERO, 64);
        assertUnsignedIntAdd("1", "1267650600228229401496703205375", ZERO, 100);
        assertUnsignedIntAdd("1267650600228229401496703205375", "1", ZERO, 100);
        assertUnsignedIntAdd("1", "680564733841876926926749214863536422911", ZERO, 129);
        assertUnsignedIntAdd("680564733841876926926749214863536422911", "1", ZERO, 129);
        assertUnsignedIntAdd("1", "8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503", ZERO, 302);
        assertUnsignedIntAdd("8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503", "1", ZERO, 302);

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

    @Test
    public void testExactSubtractPositiveNumbers() {
        assertUnsignedIntSubtract("1", "1", ZERO, 3);
        assertUnsignedIntSubtract("1", "1", ZERO, 8);
        assertUnsignedIntSubtract("1", "1", ZERO, 10);
        assertUnsignedIntSubtract("1", "1", ZERO, 16);
        assertUnsignedIntSubtract("1", "1", ZERO, 23);
        assertUnsignedIntSubtract("1", "1", ZERO, 32);
        assertUnsignedIntSubtract("1", "1", ZERO, 61);
        assertUnsignedIntSubtract("1", "1", ZERO, 64);
        assertUnsignedIntSubtract("1", "1", ZERO, 100);
        assertUnsignedIntSubtract("1", "1", ZERO, 129);
        assertUnsignedIntSubtract("1", "1", ZERO, 302);

        assertUnsignedIntSubtract("7", "3", "4", 3);
        assertUnsignedIntSubtract("255", "26", "229", 8);
        assertUnsignedIntSubtract("1023", "446", "577", 10);
        assertUnsignedIntSubtract("65535", "58198", "7337", 16);
        assertUnsignedIntSubtract("8388607", "1170914", "7217693", 23);
        assertUnsignedIntSubtract("4294967295", "1371315073", "2923652222", 32);
        assertUnsignedIntSubtract("2305843009213693951", "5649518664056373", "2300193490549637578", 61);
        assertUnsignedIntSubtract("18446744073709551615", "2143445537242890603", "16303298536466661012", 64);
        assertUnsignedIntSubtract("1267650600228229401496703205375", "173756170408016449657554666552",
                "1093894429820212951839148538823", 100);
        assertUnsignedIntSubtract("680564733841876926926749214863536422911", "224465141406699550572103541791425401294",
                "456099592435177376354645673072111021617", 129);
        assertUnsignedIntSubtract("8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503",
                "6059138187594756476463496447995278651769600770325873366650490915999905105498280100537055662",
                "2089005717743187868610286305642233992436272804337871635894070881417620093555066724196533841", 302);
    }

    @Test
    public void testExactSubtractZero() {
        assertUnsignedIntSubtract(ZERO, ZERO, ZERO, 3);
        assertUnsignedIntSubtract(ZERO, ZERO, ZERO, 8);
        assertUnsignedIntSubtract(ZERO, ZERO, ZERO, 10);
        assertUnsignedIntSubtract(ZERO, ZERO, ZERO, 16);
        assertUnsignedIntSubtract(ZERO, ZERO, ZERO, 23);
        assertUnsignedIntSubtract(ZERO, ZERO, ZERO, 32);
        assertUnsignedIntSubtract(ZERO, ZERO, ZERO, 61);
        assertUnsignedIntSubtract(ZERO, ZERO, ZERO, 64);
        assertUnsignedIntSubtract(ZERO, ZERO, ZERO, 100);
        assertUnsignedIntSubtract(ZERO, ZERO, ZERO, 129);
        assertUnsignedIntSubtract(ZERO, ZERO, ZERO, 302);

        assertUnsignedIntSubtract("1", ZERO, "1", 3);
        assertUnsignedIntSubtract("251", ZERO, "251", 8);
        assertUnsignedIntSubtract("766", ZERO, "766", 10);
        assertUnsignedIntSubtract("50009", ZERO, "50009", 16);
        assertUnsignedIntSubtract("1528604", ZERO, "1528604", 23);
        assertUnsignedIntSubtract("1741012030", ZERO, "1741012030", 32);
        assertUnsignedIntSubtract("556725238381104871", ZERO, "556725238381104871", 61);
        assertUnsignedIntSubtract("11648190819095357929", ZERO, "11648190819095357929", 64);
        assertUnsignedIntSubtract("793144184098755015219606036646", ZERO, "793144184098755015219606036646", 100);
        assertUnsignedIntSubtract("193008301982493822547037888089741100624", ZERO,
                "193008301982493822547037888089741100624", 129);
        assertUnsignedIntSubtract("4713602538052974478670041630079720222688762980104763435984356897538571477011304482016151745",
                ZERO, "4713602538052974478670041630079720222688762980104763435984356897538571477011304482016151745", 302);

        assertUnsignedIntSubtract("7", ZERO, "7", 3);
        assertUnsignedIntSubtract("255", ZERO, "255", 8);
        assertUnsignedIntSubtract("1023", ZERO, "1023", 10);
        assertUnsignedIntSubtract("65535", ZERO, "65535", 16);
        assertUnsignedIntSubtract("8388607", ZERO, "8388607", 23);
        assertUnsignedIntSubtract("4294967295", ZERO, "4294967295", 32);
        assertUnsignedIntSubtract("2305843009213693951", ZERO, "2305843009213693951", 61);
        assertUnsignedIntSubtract("18446744073709551615", ZERO, "18446744073709551615", 64);
        assertUnsignedIntSubtract("1267650600228229401496703205375", ZERO, "1267650600228229401496703205375", 100);
        assertUnsignedIntSubtract("680564733841876926926749214863536422911", ZERO,
                "680564733841876926926749214863536422911", 129);
        assertUnsignedIntSubtract("8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503",
                ZERO, "8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503", 302);
    }

    @Test
    public void testOverflowSubtract() {
        assertUnsignedIntSubtract(ZERO, "1", "7", 3);
        assertUnsignedIntSubtract(ZERO, "1", "255", 8);
        assertUnsignedIntSubtract(ZERO, "1", "1023", 10);
        assertUnsignedIntSubtract(ZERO, "1", "65535", 16);
        assertUnsignedIntSubtract(ZERO, "1", "8388607", 23);
        assertUnsignedIntSubtract(ZERO, "1", "4294967295", 32);
        assertUnsignedIntSubtract(ZERO, "1", "2305843009213693951", 61);
        assertUnsignedIntSubtract(ZERO, "1", "18446744073709551615", 64);
        assertUnsignedIntSubtract(ZERO, "1", "1267650600228229401496703205375", 100);
        assertUnsignedIntSubtract(ZERO, "1", "680564733841876926926749214863536422911", 129);
        assertUnsignedIntSubtract(ZERO, "1", "8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503", 302);

        assertUnsignedIntSubtract(ZERO, "7", "1", 3);
        assertUnsignedIntSubtract(ZERO, "255", "1", 8);
        assertUnsignedIntSubtract(ZERO, "1023", "1", 10);
        assertUnsignedIntSubtract(ZERO, "65535", "1", 16);
        assertUnsignedIntSubtract(ZERO, "8388607", "1", 23);
        assertUnsignedIntSubtract(ZERO, "4294967295", "1", 32);
        assertUnsignedIntSubtract(ZERO, "2305843009213693951", "1", 61);
        assertUnsignedIntSubtract(ZERO, "18446744073709551615", "1", 64);
        assertUnsignedIntSubtract(ZERO, "1267650600228229401496703205375", "1", 100);
        assertUnsignedIntSubtract(ZERO, "680564733841876926926749214863536422911", "1", 129);
        assertUnsignedIntSubtract(ZERO, "8148143905337944345073782753637512644205873574663745002544561797417525199053346824733589503", "1", 302);

        assertUnsignedIntSubtract("3", "4", "7", 3);
        assertUnsignedIntSubtract("150", "200", "206", 8);
        assertUnsignedIntSubtract("486", "562", "948", 10);
        assertUnsignedIntSubtract("1983", "63925", "3594", 16);
        assertUnsignedIntSubtract("8110232", "8342568", "8156272", 23);
        assertUnsignedIntSubtract("1475664880", "2163144483", "3607487693", 32);
        assertUnsignedIntSubtract("1739491374750951824", "1997266689506505973", "2048067694458139803", 61);
        assertUnsignedIntSubtract("9378141232389999287", "16375429930783797446", "11449455375315753457", 64);
        assertUnsignedIntSubtract("471071509531584910766157893628", "1221148779663667410725806798332", "517573330096146901537054300672", 100);
        assertUnsignedIntSubtract("298788250535381151463896396925003539614", "669203048573930932835085939195733487997", "310149935803327145555559672592806474529", 129);
        assertUnsignedIntSubtract("7334471909493991623082933840732849189318823740531835533227127635752908260049502045524571671",
                "7536781445200343979938801787250818113319629124390202590613387229184433862061509790601007513",
                "7945834369631591988217914807119543720205068190805377945158302203985999597041339079657153662", 302);
    }
}
