package application.sudoku_solver.Solver.Reporter;

import java.util.ArrayList;
import java.util.List;

public class Report {

    public String initialPuzzle;
    public String puzzleSolution;
    public Long timeElapsed;
    public final List<Action> actions = new ArrayList<>();
}
