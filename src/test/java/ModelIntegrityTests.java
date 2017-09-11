import edu.mayo.omopindexer.model.CDMDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * JUnit tests verifying constraints on data models in the {@link edu.mayo.omopindexer.model} package
 */
public class ModelIntegrityTests {
    /**
     * Verifies that any changes made to the {@link CDMDate.CDMDate_Type} enumuration were
     * properly handled in {@link CDMDate#getStandardizedDate()}
     */
    @Test public void verifyDateTypeConstraints() {
        StringBuilder missingTypes = new StringBuilder();
        boolean flag = false;
        for (CDMDate.CDMDate_Type type : CDMDate.CDMDate_Type.values()) {
            CDMDate date = new CDMDate(null, null, type, null);
            try {
                date.getStandardizedDate();
            } catch (RuntimeException e) {
                if (flag) {
                    missingTypes.append(", ");
                }
                flag = true;
                missingTypes.append(type.getTypeName());
            }
        }
        Assert.assertFalse("Additions to CDMDate.CDMDate_Type enumeration "
                + missingTypes.toString() + " were not properly handled in CDMDate#getStandardizedDate()!", flag);
    }
}
