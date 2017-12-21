package org.ohnlp.ir.emirs.controllers;

import org.ohnlp.ir.emirs.model.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView resetIndex() {
        ModelMap model = new ModelMap();
        model.addAttribute("query", new Query());
        return new ModelAndView("index", model);
    }

}
