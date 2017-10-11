import edu.mayo.omopindexer.model.CDMDate;
import edu.mayo.omopindexer.vocabs.SNOMEDCTUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * JUnit tests verifying utility methods in {@link edu.mayo.omopindexer.vocabs.SNOMEDCTUtils}
 */
public class SnomedCTTests {
    /**
     * Test for {@link edu.mayo.omopindexer.vocabs.SNOMEDCTUtils#isChild(String, String)}
     */
    @Test
    public void testHierarchy() {
        // Test true case (direct parent)
        Assert.assertTrue(SNOMEDCTUtils.isChild("419303009", "419492006"));
        // Test reverse of true case (parent->child)
        Assert.assertFalse(SNOMEDCTUtils.isChild("419492006", "419303009"));
        // Test true case (indirect parent)
        Assert.assertTrue(SNOMEDCTUtils.isChild("25064002", "404684003"));
        // Test reverse of indirect true case (parent->child)
        Assert.assertFalse(SNOMEDCTUtils.isChild("404684003", "25064002"));
    }
}
