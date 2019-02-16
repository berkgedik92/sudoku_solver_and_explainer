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

        SudokuHandler sudokuHandler = new SudokuHandler(data);

        try {
            sudokuHandler.run();
            sudokuHandler.join();
        } catch (InterruptedException e) {
            System.err.println("ERROR ON CONTROLLER!");
            e.printStackTrace();
        }

        return sudokuHandler.getReport();
    }
}
