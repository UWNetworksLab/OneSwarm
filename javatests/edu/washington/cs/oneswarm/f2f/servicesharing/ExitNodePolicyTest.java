package edu.washington.cs.oneswarm.f2f.servicesharing;

import static org.junit.Assert.*;
import org.junit.*;

import java.util.Date;

import edu.washington.cs.oneswarm.test.util.OneSwarmTestBase;

/**
 * Tests ExitNodeInfo, verifying that it correctly represents the policy of the
 * server it represents.
 * 
 * @author Nick
 * 
 */
public class ExitNodePolicyTest extends OneSwarmTestBase {

    @Test
    public void testExitNodeInfo() throws Exception {
        /*
         * Verify that the exit policy passed to the ExitNodeInfo is correctly
         * represented in subsequent calls to 'allowsConnection(String url, int
         * port)'
         * 
         * Test plan: -Create a ExitNodeInfo with a complex exit policy -Verify
         * that the results match the set of manually predetermined results for
         * each case
         */

        try {
            String[] policy = new String[] { "reject yahoo.com", "accept *:80",
                    "reject *.google.com:40", "accept google.com:40", "reject *.2.*.*:40",
                    "accept 4.*.2.2:40", "reject *:*" };

            ExitNodeInfo server = new ExitNodeInfo("Servo The Magnificent", 123456789, 275, policy,
                    new Date(), "Version string 2.0");

            // Sample Url's to test
            String[] urls = new String[] { "google.com", "yahoo.com", "maps.google.com", "4.2.2.2",
                    "4.5.2.2" };

            // Sample ports to test
            int[] ports = new int[] { 80, 40 };

            // First index is url, second index is port
            boolean[][] expected = new boolean[][] { { true, true }, { false, false },
                    { true, false }, { true, false }, { true, true } };

            boolean[][] actual = new boolean[urls.length][ports.length];
            for (int x = 0; x < urls.length; x++) {
                for (int y = 0; y < ports.length; y++) {
                    actual[x][y] = server.allowsConnectionTo(urls[x], ports[y]);
                }
            }

            assertArrayEquals(expected, actual);

        } catch (Exception e) {
            fail(e.toString());
        }
    }
}
