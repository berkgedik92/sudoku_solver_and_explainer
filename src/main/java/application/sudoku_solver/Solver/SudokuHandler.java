package application.sudoku_solver.Solver;

import application.sudoku_solver.Solver.Reporter.Report;
import application.sudoku_solver.Solver.Models.Sudoku;

public class SudokuHandler extends Thread {

    private final String data;
    public Report report;

    public SudokuHandler(String data) {
        this.data = data;
    }

    @Override
    public void run() {
        Sudoku sudoku = new Sudoku(15);

        String[] lines = new String[9];
        for (int row = 0; row < 9; row++) {
            lines[row] = data.substring(row * 9, (row + 1) * 9);
        }

        for (int row = 0; row < 9; row++)
        {
            String line = lines[row];
            for (int col = 0; col < 9; col++) {
                if (line.charAt(col) != 'x')
                    sudoku.setNumberForCell(row, col,line.charAt(col) - '0');
            }
            sudoku.giveLineNow(line);
        }

        sudoku.solve();

        String solution = sudoku.getSolution();
        report = sudoku.getReport();
    }
}
