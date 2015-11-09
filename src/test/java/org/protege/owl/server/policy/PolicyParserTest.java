package org.protege.owl.server.policy;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.protege.owl.server.api.server.ServerPath;
import org.protege.owl.server.policy.generated.PolicyLexer;
import org.protege.owl.server.policy.generated.PolicyParser;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;

import static org.protege.owl.server.TestUtilities.*;

public class PolicyParserTest {
    public static final ServerPath FERGERSON_PIZZA = new ServerPath("/pizza-fergerson.history");
    public static final ServerPath REDMONDS_DIR    = new ServerPath("/redmonds-private-directory");
    
    @Test
    public void testPolicy00() throws RecognitionException, IOException {
        Policy policy = parsePolicy("src/test/resources/parser/Policy01");
        UserDatabase userDb = UserParserTest.parseUserDatabase("src/test/resources/parser/UsersAndGroups01");
        
        Assert.assertTrue(policy.checkPermission(userDb, VENDETTI, FERGERSON_PIZZA, Operation.READ));
        Assert.assertTrue(policy.checkPermission(userDb, VENDETTI, FERGERSON_PIZZA, Operation.WRITE));
        
        Assert.assertTrue(policy.checkPermission(userDb, GUEST, FERGERSON_PIZZA, Operation.READ));
        Assert.assertFalse(policy.checkPermission(userDb, GUEST, FERGERSON_PIZZA, Operation.WRITE));
        
        Assert.assertTrue(policy.checkPermission(userDb, FERGERSON, FERGERSON_PIZZA, Operation.WRITE));
    }
    
    @Test
    public void testPolicy01() throws RecognitionException, IOException {
        Policy policy = parsePolicy("src/test/resources/configuration.03/Policy");
        UserDatabase userDb = UserParserTest.parseUserDatabase("src/test/resources/parser/UsersAndGroups01");

        Assert.assertFalse(policy.checkPermission(userDb, GUEST, REDMONDS_DIR, Operation.READ));
    }
    
    @Test
    public void roundTripTest() throws RecognitionException, IOException {
        roundTripTest("src/test/resources/parser/Policy01");
    }
    
    private void roundTripTest(String fileName) throws RecognitionException, IOException {
        Policy p = parsePolicy(fileName);
        File tmp = File.createTempFile("Policy", ".policy");
        Writer tmpWriter = new FileWriter(tmp);
        try {
            p.write(tmpWriter);
        }
        finally {
            tmpWriter.flush();
            tmpWriter.close();
        }
        Policy p2 = parsePolicy(tmp.getAbsolutePath());
        Assert.assertEquals(p2, p);
        tmp.delete();
    }
    
    private Policy parsePolicy(String fileName) throws RecognitionException, IOException {
        File policyFile = new File(fileName);
        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(policyFile));
        PolicyLexer lexer = new PolicyLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PolicyParser parser = new PolicyParser(tokens);
        parser.top();
        return parser.getPolicy();
    }
}
