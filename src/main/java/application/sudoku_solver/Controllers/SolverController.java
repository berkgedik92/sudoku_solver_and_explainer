package application.sudoku_solver.Controllers;

import application.sudoku_solver.Solver.Reporter.Report;
import application.sudoku_solver.Solver.SudokuHandler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/solver")
class SolverController {

    @CrossOrigin
    @PostMapping(value = "/solve")
    public Report solve(@RequestBody String data) {
        SudokuHandler handler = new SudokuHandler(data);
        handler.run();
        try {
            handler.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return handler.report;
    }
}
