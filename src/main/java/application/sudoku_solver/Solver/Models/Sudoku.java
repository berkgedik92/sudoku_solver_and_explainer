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
    private final int threadAmount;
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

        this.threadAmount = threadAmount;
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
        for (int i = 0; i < threadAmount; i++)
            solvers.get(i).start();

        for (int i = 0; i < threadAmount; i++) {
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

    /*
        This is the function that a solver calls to attempt to stop. A solver might want to stop for two
        reasons: 1) It could neither get a new date nor eliminate any candidate. 2) There is a contradiction

        For case (1), the thread needs to sleep and wait until a new data comes (i.e another solver eliminates
        some candidates from a cell which this solver is also responsible from)

        For case (2), all threads should sleep and then then the puzzle must be restored to the latest
        state where there is no contradiction. Then all solvers will wake up and try to solve the problem.

        For any case, if all threads are stopped and this is the last thread, it will not be stopped.
     */
    public boolean threadStopAttempt() {

        // Assume that it is not the last thread (if it is the last thread we will not stop it)
        boolean shouldThreadBeStopped = true;

        synchronized (myMutex) {

            workingThread--;
            if (workingThread == 0) {
                System.out.println("All threads except one are stopped. Now we will check how to proceed...");

                /*
                    Case (1) Contradiction: If there is a contradiction, check if there is any non-contradicting state
                    in the stack. If so restore the state and wake up all solvers. Otherwise we conclude that the puzzle
                    is not solvable. In this case, just kill the threads
                 */
                if (foundContradiction) {

                    // If there is no non-contradicting state in the stack, kill the threads...
                    if (attempts.empty()) {
                        for (int i = 0; i < threadAmount; i++)
                            solvers.get(i).killThread();
                    }

                    // If there is a state, return to that state and wake up all threads...
                    else {
                        // Restore
                        foundContradiction = false;
                        Action action = new Action(attempts.peek().changedCell, 0, EliminationEvent.ROLLBACK, attempts.peek().pickedValue);
                        for (int i = 0; i < 81; i++) {
                            cells[i].assign(attempts.peek().values[i]);
                            action.values[i] = attempts.peek().values[i];
                        }

                        /*
                            Remember what candidate we picked for the cell when we made the random guess. Now as we
                            know that this candidate led us into a contradiction, we conclude that this cannot be a
                            candidate so we will remove this value from the candidates list of the cell
                        */

                        cells[attempts.peek().changedCell].remove(attempts.peek().pickedValue);
                        addToQueue(action);
                        attempts.pop();

                        // Signal all threads to run again
                        workingThread = threadAmount;
                        for (int i = 0; i < threadAmount; i++)
                            solvers.get(i).signalToWork();
                    }
                }
                /*
                    Case (2) There is no thread which could eliminate any candidate.

                    This is possible only in two cases:
                        1) The puzzle is solved (so there is no more candidates to eliminate)
                        2) The solvers cannot eliminate anything using the elimination logic we have implemented

                        For case (1) we should kill all the threads
                        For case (2) we must save the current state into the stack, make a guess (pick a candidate
                        randomly for a cell)
                 */
                else {
                    int isNotFinished = isThereMoreThanOneCandidate();

                    // Case (1) : The puzzle is solved...
                    if (isNotFinished == -1) {
                        System.out.println("Puzzle is solved!");
                        for (int i = 0; i < threadAmount; i++)
                            solvers.get(i).killThread();
                    }
                    // Case (2) : We will make a guess
                    else {
                        System.out.println("We have to make a guess!");

                        SudokuBackup backup = new SudokuBackup();
                        for (int i = 0; i < 81; i++)
                            backup.values[i] = cells[i].getCandidates();

                        backup.pickedValue = cells[isNotFinished].pickCandidate();
                        backup.changedCell = isNotFinished;
                        Action action = new Action(isNotFinished, 0, EliminationEvent.TRYING, backup.pickedValue);
                        addToQueue(action);
                        attempts.push(backup);

                        // Run the threads again
                        workingThread = threadAmount;
                        for (int i = 0; i < threadAmount; i++) {
                            solvers.get(i).signalToWork();
                        }
                    }
                }
                shouldThreadBeStopped = false;
            }
        }
        return shouldThreadBeStopped;
    }

    public void continueWork() {
        synchronized (myMutex) {
            if (!foundContradiction) {
                workingThread = threadAmount;
                for (int i = 0; i < threadAmount; i++) {
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

        for (int i = 0; i < threadAmount; i++)
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