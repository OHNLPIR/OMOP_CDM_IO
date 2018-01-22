package org.ohnlp.ir.emirs.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ohnlp.ir.emirs.Properties;
import org.ohnlp.ir.emirs.model.MappingDefinition;
import org.ohnlp.ir.emirs.model.ObjectMapping;
import org.ohnlp.ir.emirs.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

@Controller
public class IndexController {

    private Properties properties;
    private RestTemplate REST_CLIENT = new RestTemplate();
    private String ES_REST_URL = null;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView resetIndex() {
        ModelMap model = new ModelMap();
        model.addAttribute("query", new Query());
        return new ModelAndView("index", model);
    }

    @RequestMapping(value = "/_mappings", method = RequestMethod.GET)
    public @ResponseBody MappingDefinition getMappings() {
        // Initialize values from properties if needed
        if (ES_REST_URL == null) {
            ES_REST_URL = "http://" + properties.getEs().getHost() + ":" + properties.getEs().getHttpport() + "/"
                    + properties.getEs().getIndexName();
        }
        ObjectNode idxes = REST_CLIENT.getForObject(ES_REST_URL, ObjectNode.class);
        JsonNode mapping = idxes.path(properties.getEs().getIndexName()); // If not an alias
        if (mapping == null) {
            // Possible definition as alias in config
            Iterator<JsonNode> nodes = idxes.elements();
            while (nodes.hasNext() && mapping == null) {
                JsonNode node = nodes.next();
                JsonNode aliases = node.get("aliases");
                if (aliases != null) {
                    for (Iterator<String> it = aliases.fieldNames(); it.hasNext(); ) {
                        String idxName = it.next();
                        if (idxName.equalsIgnoreCase(properties.getEs().getIndexName())) {
                            mapping = node;
                            break;
                        }
                    }
                }
            }
        }
        MappingDefinition ret = new MappingDefinition();
        if (mapping != null) {
            try {
                MappingDefinition.IndexDefinition def = new ObjectMapper().treeToValue(mapping, MappingDefinition.IndexDefinition.class);
                ret.setIndex(def);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }
    public Properties getProperties() {
        return properties;
    }

    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}

