package application.sudoku_solver.Solver;

import application.sudoku_solver.Solver.Reporter.Report;
import application.sudoku_solver.Solver.Models.Sudoku;

public class SudokuHandler extends Thread {

    private static final int THREAD_AMOUNT = 15;
    public static final boolean LOG_LOCK_ACQUIREMENTS = true;
    public static final boolean LOG_THREAD_ACTIONS = true;

    private final String data;
    private Report report;

    public SudokuHandler(String data) {
        this.data = data;
    }

    @Override
    public void run() {
        Sudoku sudoku = new Sudoku(THREAD_AMOUNT);

        for (int row = 0; row < 9; row++) {
            String line = data.substring(row * 9, (row + 1) * 9);

            for (int col = 0; col < 9; col++)
                if (line.charAt(col) != 'x')
                    sudoku.setNumberForCell(row, col,line.charAt(col) - '0');
        }

        sudoku.solve();

        report = sudoku.getReport();
        report.initialPuzzle = data;
        report.puzzleSolution = sudoku.getSolution();
    }

    public Report getReport() {
        return report;
    }
}
