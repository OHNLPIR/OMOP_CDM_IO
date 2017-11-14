package java;

import edu.mayo.bsi.semistructuredir.cdm.controllers.TopicSearchController;
import org.junit.Test;
import org.springframework.ui.ModelMap;

/**
 * Just a quick undocumented demonstration test
 */
public class SearchTest {
    @Test
    public void run() {
        new TopicSearchController().handleTopicRequest(new ModelMap(), "1","2","3","4","5");
    }
}
