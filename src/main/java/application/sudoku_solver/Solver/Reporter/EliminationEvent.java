package application.sudoku_solver.Solver.Reporter;

public enum EliminationEvent {
    NAKED_ELIMINATION,
    HIDDEN_ELIMINATION,
    BOX_REDUCTION,
    POINTING_PAIR,
    TRYING,
    ROLLBACK,
    CONTRADICTION
}
