package application.sudoku_solver.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/*
http://localhost:8080/index/xxx3xxx2xxx7xx58xxx8x4xxxxx7x3xxxxx6x5xx2xx8x2xxxxx7x1xxxxx9x1xxx47xx3xxx2xxx8xxx
 */

//Controller that returns views (JSP pages)
@Controller
@RequestMapping("")
class ViewsController {

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public ModelAndView getSolverPage() {

        ModelAndView view = new ModelAndView("solver.html");
        view.addObject("solution", "");
        return view;
    }
}