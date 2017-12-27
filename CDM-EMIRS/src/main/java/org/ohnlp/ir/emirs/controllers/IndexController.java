package org.ohnlp.ir.emirs.controllers;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

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
        return REST_CLIENT.getForObject(ES_REST_URL, MappingDefinition.class);
    }
    public Properties getProperties() {
        return properties;
    }

    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}

