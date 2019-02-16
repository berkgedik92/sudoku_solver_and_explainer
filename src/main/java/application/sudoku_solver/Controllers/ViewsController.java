package application.sudoku_solver.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("")
class ViewsController {

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public ModelAndView getSolverPage() {
        return new ModelAndView("solver.html");
    }
}