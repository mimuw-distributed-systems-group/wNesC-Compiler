package pl.edu.mimuw.nesc.constexpr.value;

import org.junit.Test;
import static pl.edu.mimuw.nesc.constexpr.value.ConstantValueAssert.*;

/**
 * <p>Bits counts used for tests:</p>
 * <ul>
 *     <li>numbers divisible by 8:</li>
 *     <ul>
 *         <li>8</li>
 *         <li>16</li>
 *         <li>32</li>
 *         <li>64</li>
 *     </ul>
 *     <li>numbers not divisible by 8:</li>
 *     <ul>
 *         <li>129 (129 &equiv; 1 mod 8)</li>
 *         <li>10 (10 &equiv; 2 mod 8)</li>
 *         <li>3</li>
 *         <li>100 (100 &equiv; 4 mod 8)</li>
 *         <li>61 (61 &equiv; 5 mod 8)</li>
 *         <li>302 (302 &equiv; 6 mod 8)</li>
 *         <li>23 (23 &equiv; 7 mod 8)</li>
 *     </ul>
 * </ul>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class SignedIntegerConstantValueTest {
    private static final String ZERO = "0";

    @Test
    public void testExactAddPositiveNumbers() {
        assertSignedIntAdd("1", "1", "2", 3);
        assertSignedIntAdd("1", "1", "2", 8);
        assertSignedIntAdd("1", "1", "2", 10);
        assertSignedIntAdd("1", "1", "2", 16);
        assertSignedIntAdd("1", "1", "2", 23);
        assertSignedIntAdd("1", "1", "2", 32);
        assertSignedIntAdd("1", "1", "2", 61);
        assertSignedIntAdd("1", "1", "2", 64);
        assertSignedIntAdd("1", "1", "2", 100);
        assertSignedIntAdd("1", "1", "2", 129);
        assertSignedIntAdd("1", "1", "2", 302);

        assertSignedIntAdd("5", "2", "7", 4);
        assertSignedIntAdd("120", "7", "127", 8);
        assertSignedIntAdd("250", "261", "511", 10);
        assertSignedIntAdd("32000", "767", "32767", 16);
        assertSignedIntAdd("1000000", "3194303", "4194303", 23);
        assertSignedIntAdd("2140000000", "7483647", "2147483647", 32);
        assertSignedIntAdd("977263594556318015", "175657910050528960", "1152921504606846975", 61);
        assertSignedIntAdd("7", "9223372036854775800", "9223372036854775807", 64);
        assertSignedIntAdd("473500027314849", "633825300114114227248324287838", "633825300114114700748351602687", 100);
        assertSignedIntAdd("8567858", "340282366920938463463374607431759643597", "340282366920938463463374607431768211455", 129);
        assertSignedIntAdd("2037035976334486086268445688409378161051468393665936250636140449354381299763336706183397375",
                "2037035976334486086268445688409378161051468393665936250636140449354381299763336706183397376",
                "4074071952668972172536891376818756322102936787331872501272280898708762599526673412366794751",
                302);
    }

    @Test
    public void testExactAddNegativeNumbers() {
        assertSignedIntAdd("-1", "-2", "-3", 3);
        assertSignedIntAdd("-1", "-2", "-3", 8);
        assertSignedIntAdd("-1", "-2", "-3", 10);
        assertSignedIntAdd("-1", "-2", "-3", 16);
        assertSignedIntAdd("-1", "-2", "-3", 23);
        assertSignedIntAdd("-1", "-2", "-3", 32);
        assertSignedIntAdd("-1", "-2", "-3", 61);
        assertSignedIntAdd("-1", "-2", "-3", 64);
        assertSignedIntAdd("-1", "-2", "-3", 100);
        assertSignedIntAdd("-1", "-2", "-3", 129);
        assertSignedIntAdd("-1", "-2", "-3", 302);

        assertSignedIntAdd("-1", "-3", "-4", 3);
        assertSignedIntAdd("-20", "-108", "-128", 8);
        assertSignedIntAdd("-73", "-439", "-512", 10);
        assertSignedIntAdd("-9407", "-23361", "-32768", 16);
        assertSignedIntAdd("-1193553", "-3000751", "-4194304", 23);
        assertSignedIntAdd("-274017709", "-1873465939", "-2147483648", 32);
        assertSignedIntAdd("-222867653814149857", "-930053850792697119", "-1152921504606846976", 61);
        assertSignedIntAdd("-974793042526489436", "-8248578994328286372", "-9223372036854775808", 64);
        assertSignedIntAdd("-136510475638075141600191944885", "-497314824476039559148159657803",
                "-633825300114114700748351602688", 100);
        assertSignedIntAdd("-79361994939516741328970781152213941036", "-260920371981421722134403826279554270420",
                "-340282366920938463463374607431768211456", 129);
        assertSignedIntAdd("-842892484996806590713538162509896779697422199040236775416937216407494767697322982224702694",
                "-3231179467672165581823353214308859542405514588291635725855343682301267831829350430142092058",
                "-4074071952668972172536891376818756322102936787331872501272280898708762599526673412366794752",
                302);
    }

    @Test
    public void testExactAddZero() {
        assertSignedIntAdd("1", ZERO, "1", 3);
        assertSignedIntAdd(ZERO, "1", "1", 8);
        assertSignedIntAdd(ZERO, "1", "1", 10);
        assertSignedIntAdd("1", ZERO, "1", 16);
        assertSignedIntAdd(ZERO, "1", "1", 23);
        assertSignedIntAdd(ZERO, "1", "1", 32);
        assertSignedIntAdd("1", ZERO, "1", 61);
        assertSignedIntAdd("1", ZERO, "1", 64);
        assertSignedIntAdd("1", ZERO, "1", 100);
        assertSignedIntAdd("1", ZERO, "1", 129);
        assertSignedIntAdd("1", ZERO, "1", 302);

        assertSignedIntAdd(ZERO, "-2", "-2", 3);
        assertSignedIntAdd(ZERO, "-2", "-2", 8);
        assertSignedIntAdd("-1", ZERO, "-1", 10);
        assertSignedIntAdd(ZERO, "-2", "-2", 16);
        assertSignedIntAdd(ZERO, "-2", "-2", 23);
        assertSignedIntAdd(ZERO, "-2", "-2", 32);
        assertSignedIntAdd(ZERO, "-2", "-2", 61);
        assertSignedIntAdd("-1", ZERO, "-1", 64);
        assertSignedIntAdd("-1", ZERO, "-1", 100);
        assertSignedIntAdd("-1", ZERO, "-1", 129);
        assertSignedIntAdd(ZERO, "-2", "-2", 302);

        assertSignedIntAdd(ZERO, "2", "2", 4);
        assertSignedIntAdd(ZERO, "7", "7", 8);
        assertSignedIntAdd("250", ZERO, "250", 10);
        assertSignedIntAdd(ZERO, "767", "767", 16);
        assertSignedIntAdd("1000000", ZERO, "1000000", 23);
        assertSignedIntAdd(ZERO, "7483647", "7483647", 32);
        assertSignedIntAdd("977263594556318015", ZERO, "977263594556318015", 61);
        assertSignedIntAdd(ZERO, "9223372036854775800", "9223372036854775800", 64);
        assertSignedIntAdd("473500027314849", ZERO, "473500027314849", 100);
        assertSignedIntAdd("8567858", ZERO, "8567858", 129);
        assertSignedIntAdd("2037035976334486086268445688409378161051468393665936250636140449354381299763336706183397375",
                ZERO,
                "2037035976334486086268445688409378161051468393665936250636140449354381299763336706183397375",
                302);

        assertSignedIntAdd("-1", ZERO, "-1", 3);
        assertSignedIntAdd("-20", ZERO, "-20", 8);
        assertSignedIntAdd("-73", ZERO, "-73", 10);
        assertSignedIntAdd("-9407", ZERO, "-9407", 16);
        assertSignedIntAdd("-1193553", ZERO, "-1193553", 23);
        assertSignedIntAdd(ZERO, "-1873465939", "-1873465939", 32);
        assertSignedIntAdd(ZERO, "-930053850792697119", "-930053850792697119", 61);
        assertSignedIntAdd("-974793042526489436", ZERO, "-974793042526489436", 64);
        assertSignedIntAdd(ZERO, "-497314824476039559148159657803",
                "-497314824476039559148159657803", 100);
        assertSignedIntAdd(ZERO, "-260920371981421722134403826279554270420",
                "-260920371981421722134403826279554270420", 129);
        assertSignedIntAdd("-842892484996806590713538162509896779697422199040236775416937216407494767697322982224702694",
                ZERO,
                "-842892484996806590713538162509896779697422199040236775416937216407494767697322982224702694",
                302);
    }

    @Test
    public void testExactAddMixed() {
        assertSignedIntAdd("-1", "1", ZERO, 3);
        assertSignedIntAdd("-1", "1", ZERO, 8);
        assertSignedIntAdd("1", "-1", ZERO, 10);
        assertSignedIntAdd("-1", "1", ZERO, 16);
        assertSignedIntAdd("-1", "1", ZERO, 23);
        assertSignedIntAdd("-1", "1", ZERO, 32);
        assertSignedIntAdd("1", "-1", ZERO, 61);
        assertSignedIntAdd("1", "-1", ZERO, 64);
        assertSignedIntAdd("-1", "1", ZERO, 100);
        assertSignedIntAdd("1", "-1", ZERO, 129);
        assertSignedIntAdd("-1", "1", ZERO, 302);

        assertSignedIntAdd("-4", "3", "-1", 3);
        assertSignedIntAdd("127", "-128", "-1", 8);
        assertSignedIntAdd("-512", "511", "-1", 10);
        assertSignedIntAdd("-32768", "32767", "-1", 16);
        assertSignedIntAdd("4194303", "-4194304", "-1", 23);
        assertSignedIntAdd("-2147483648", "2147483647", "-1", 32);
        assertSignedIntAdd("1152921504606846975", "-1152921504606846976", "-1", 61);
        assertSignedIntAdd("-9223372036854775808", "9223372036854775807", "-1", 64);
        assertSignedIntAdd("633825300114114700748351602687", "-633825300114114700748351602688", "-1", 100);
        assertSignedIntAdd("-340282366920938463463374607431768211456", "340282366920938463463374607431768211455",
                "-1", 129);
        assertSignedIntAdd("4074071952668972172536891376818756322102936787331872501272280898708762599526673412366794751",
                "-4074071952668972172536891376818756322102936787331872501272280898708762599526673412366794752",
                "-1", 302);
    }

    @Test
    public void testOverflowAdd() {
        assertSignedIntAdd("1", "3", "-4", 3);
        assertSignedIntAdd("127", "1", "-128", 8);
        assertSignedIntAdd("511", "1", "-512", 10);
        assertSignedIntAdd("1", "32767", "-32768", 16);
        assertSignedIntAdd("4194303", "1", "-4194304", 23);
        assertSignedIntAdd("2147483647", "1", "-2147483648", 32);
        assertSignedIntAdd("1", "1152921504606846975", "-1152921504606846976", 61);
        assertSignedIntAdd("1", "9223372036854775807", "-9223372036854775808", 64);
        assertSignedIntAdd("633825300114114700748351602687", "1", "-633825300114114700748351602688", 100);
        assertSignedIntAdd("1", "340282366920938463463374607431768211455", "-340282366920938463463374607431768211456", 129);
        assertSignedIntAdd("1", "4074071952668972172536891376818756322102936787331872501272280898708762599526673412366794751",
                "-4074071952668972172536891376818756322102936787331872501272280898708762599526673412366794752",
                302);

        assertSignedIntAdd("3", "3", "-2", 3);
        assertSignedIntAdd("127", "127", "-2", 8);
        assertSignedIntAdd("511", "511", "-2", 10);
        assertSignedIntAdd("32767", "32767", "-2", 16);
        assertSignedIntAdd("4194303", "4194303", "-2", 23);
        assertSignedIntAdd("2147483647", "2147483647", "-2", 32);
        assertSignedIntAdd("1152921504606846975", "1152921504606846975", "-2", 61);
        assertSignedIntAdd("9223372036854775807", "9223372036854775807", "-2", 64);
        assertSignedIntAdd("633825300114114700748351602687", "633825300114114700748351602687", "-2", 100);
        assertSignedIntAdd("340282366920938463463374607431768211455", "340282366920938463463374607431768211455", "-2", 129);
        assertSignedIntAdd("4074071952668972172536891376818756322102936787331872501272280898708762599526673412366794751",
                "4074071952668972172536891376818756322102936787331872501272280898708762599526673412366794751",
                "-2", 302);
    }
}
