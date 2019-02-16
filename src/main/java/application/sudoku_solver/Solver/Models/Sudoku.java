package application.sudoku_solver.Solver.Models;

import application.sudoku_solver.Solver.Managers.BoxManager;
import application.sudoku_solver.Solver.Managers.ColumnManager;
import application.sudoku_solver.Solver.Managers.RegionManager;
import application.sudoku_solver.Solver.Managers.RowManager;
import application.sudoku_solver.Solver.Reporter.Action;
import application.sudoku_solver.Solver.Reporter.EliminationEvent;
import application.sudoku_solver.Solver.Reporter.Report;
import application.sudoku_solver.Solver.SudokuSolver;

import java.util.*;

public class Sudoku {

    private final Stack<SudokuBackup> attempts;
    private boolean foundContradiction;
    private final Queue<Action> actions;
    private final Object myMutex = new Object();
    private final Object queueMutex = new Object();
    private final int thrAmount;
    private int workingThread;
    private final Cell[] cells;
    private final RegionManager[] regions;
    private final List<SudokuSolver> solvers;
    private String intro = "";

    public Sudoku(int threadAmount) {
        this.cells = new Cell[81];
        this.regions = new RegionManager[27];
        this.solvers = new ArrayList<>();
        this.actions = new ArrayDeque<>();
        this.attempts = new Stack<>();

        for (int i = 0; i < 81; i++)
            cells[i] = new Cell(i);

        thrAmount = threadAmount;
        workingThread = threadAmount;
        foundContradiction = false;

        for (int i = 0; i < threadAmount; i++) {
            solvers.add(new SudokuSolver(i, this));
        }

        for (int i = 0; i < 81; i += 9)
            regions[i / 9] = new RowManager(cells[i], cells[i + 1], cells[i + 2], cells[i + 3],
                    cells[i + 4], cells[i + 5], cells[i + 6], cells[i + 7], cells[i + 8],i / 9,this);

        for (int i = 0; i < 9; i++)
            regions[i + 9] = new ColumnManager(cells[i], cells[i + 9], cells[i + 18], cells[i + 27],
                    cells[i + 36], cells[i + 45], cells[i + 54], cells[i + 63], cells[i + 72],i + 9,this);

        for (int i = 0; i < 9; i++) {
            int r = (i / 3) * 3;
            int c = (i % 3) * 3;
            int ind = r * 9 + c;
            regions[i + 18] = new BoxManager(cells[ind], cells[ind + 1], cells[ind + 2], cells[ind + 9],
                    cells[ind + 10], cells[ind + 11], cells[ind + 18], cells[ind + 19], cells[ind + 20],i + 18,this);
        }

        for (int j = 0; j < 27; j++)
            solvers.get(j % threadAmount).assignData(regions[j]);
    }

    public void giveLineNow(String s) {
        intro += s;
    }

    public void setNumberForCell(int row, int col, int number) {
        char data = (char)('0' + number);
        cells[row * 9 + col].assign(data);
    }

    public void solve() {
        for (int i = 0; i < thrAmount; i++)
            solvers.get(i).start();

        for (int i = 0; i < thrAmount; i++) {
            try {
                solvers.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String getSolution() {
        StringBuilder b = new StringBuilder();
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++)
                b.append(cells[row * 9 + col].toString()).append(" ");
            b.append("\n");
        }

        return b.toString();
    }

    public boolean shouldThreadStop(int threadID) {

        boolean result;
        synchronized (myMutex) {

            workingThread--;
            if (workingThread == 0) {
                System.out.println("No working thread!");
                if (foundContradiction) {
                    if (attempts.empty()) {
                        for (int i = 0; i < thrAmount; i++) {
                            solvers.get(i).killThread();
                            solvers.get(i).signalToWork();
                        }
                    }
                    else {
                        foundContradiction = false;
                        Action action = new Action(attempts.peek().changedCell, 0, EliminationEvent.ROLLBACK, attempts.peek().pickedValue);
                        for (int i = 0; i < 81; i++) {
                            cells[i].assign(attempts.peek().values[i]);
                            action.values[i] = attempts.peek().values[i];
                        }
                        cells[attempts.peek().changedCell].remove(attempts.peek().pickedValue);
                        addToQueue(action);
                        attempts.pop();
                        workingThread = thrAmount;
                        for (int i = 0; i < thrAmount; i++) {
                            solvers.get(i).signalToWork();
                        }
                    }
                }
                else {
                    int isNotFinished = isThereMoreThanOneCandidate();
                    //It is solved...
                    if (isNotFinished == -1) {
                        System.out.println("Puzzle is solved!");
                        for (int i = 0; i < thrAmount; i++) {
                            solvers.get(i).killThread();
                            solvers.get(i).signalToWork();
                        }
                    }
                    //Guess is needed
                    else {
                        System.out.println("Will make a guess!");

                        SudokuBackup backup = new SudokuBackup();
                        for (int i = 0; i < 81; i++)
                            backup.values[i] = cells[i].getCandidates();
                        backup.pickedValue = cells[isNotFinished].pickCandidate();
                        backup.changedCell = isNotFinished;
                        Action action = new Action(isNotFinished, 0, EliminationEvent.TRYING, backup.pickedValue);
                        addToQueue(action);
                        attempts.push(backup);
                        workingThread = thrAmount;
                        for (int i = 0; i < thrAmount; i++) {
                            solvers.get(i).signalToWork();
                        }
                    }
                }
                result = false;
            }
            else {
                result = true;
            }
        }
        return result;
    }

    public void continueWork() {
        synchronized (myMutex) {
            if (!foundContradiction) {
                workingThread = thrAmount;
                for (int i = 0; i < thrAmount; i++) {
                    solvers.get(i).signalToWork();
                }
            }
        }
    }

    public void addToQueue(Action action) {
        synchronized (queueMutex) {
            actions.add(action);
        }
    }

    public Report getReport() {

        Report report = new Report();

        report.intro = intro;
        report.timeElapsed = 0L;

        boolean contradiction = false;

        while (actions.size() != 0) {

            /*if (contradiction) {
                while (actions.peek().event != 4)
                    actions.remove();
                contradiction = false;
            }*/

            Action action = actions.peek();
            actions.remove();

            report.actions.add(action);
        }

        return report;
    }

    public void signalContradiction() {
        if (foundContradiction)
            return;

        foundContradiction = true;

        for (int i = 0;i < thrAmount; i++)
            solvers.get(i).signalToStopDueToContradiction();
    }

    private int isThereMoreThanOneCandidate() {
        int result = -1;
        int minAmount = 9;
        for (int i = 0; i < 81;i++) {
            int amount = CellOperations.getCandidateAmount(cells[i].data);
            if (amount > 1 && amount < minAmount) {
                minAmount = amount;
                result = i;
            }
        }
        return result;
    }

    public Cell[] getCells(int targetID) {
        return regions[targetID].getCellRef();
    }
}