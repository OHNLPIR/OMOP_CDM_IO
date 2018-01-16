package edu.mayo.bsi.semistructuredir.topicmodeling;

import edu.mayo.bsi.semistructuredir.topicmodeling.similarity.TopicModelingScriptEngine;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.ScriptEngineService;

public class TopicModelingPlugin extends Plugin implements ScriptPlugin {
    @Override
    public ScriptEngineService getScriptEngineService(Settings settings) {
        return new TopicModelingScriptEngine();
    }
}
