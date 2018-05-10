package org.ohnlp.ir.create.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ohnlp.ir.create.Properties;
import org.ohnlp.ir.create.model.MappingDefinition;
import org.ohnlp.ir.create.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.*;

@Controller
public class IndexController {

    private Properties properties;
    private MappingDefinition mapping = null;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView resetIndex() {
        ModelMap model = new ModelMap();
        model.addAttribute("query", new Query());
        return new ModelAndView("index", model);
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "login";
    }

    @RequestMapping(value = "/_mappings", method = RequestMethod.GET)
    public @ResponseBody MappingDefinition getMappings() {
        if (mapping != null) {
            return mapping;
        } else {
            // Initialize values from properties if needed
            JsonNode idxes;
            try {
                idxes = new ObjectMapper().readTree(IndexController.class.getResourceAsStream("/index_mappings.json"));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            JsonNode mapping = idxes.get(properties.getEs().getIndexName()); // If not an alias
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
    }

    @RequestMapping(value = "/_user", method = RequestMethod.POST)
    public @ResponseBody Collection<String> getLoggedInUser() {
        return Collections.singleton(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    public Properties getProperties() {
        return properties;
    }

    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}

