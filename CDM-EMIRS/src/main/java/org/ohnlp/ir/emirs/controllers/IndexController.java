package org.ohnlp.ir.emirs.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {
    @RequestMapping("/")
    public ModelAndView resetIndex() {
        ModelMap model = new ModelMap();
        return new ModelAndView("index.jsp", model);
    }
}
