package application.sudoku_solver.Solver.Models;

import application.sudoku_solver.Solver.Managers.BoxManager;
import application.sudoku_solver.Solver.Managers.ColumnManager;
import application.sudoku_solver.Solver.Managers.RegionManager;
import application.sudoku_solver.Solver.Managers.RowManager;
import application.sudoku_solver.Solver.Reporter.Action;
import application.sudoku_solver.Solver.Reporter.EliminationEvent;
import application.sudoku_solver.Solver.Reporter.Report;
import application.sudoku_solver.Solver.SudokuHandler;
import application.sudoku_solver.Solver.SudokuSolver;

import java.util.*;

public class Sudoku {

    private final Stack<SudokuBackup> attempts;
    private boolean foundContradiction;
    private final Queue<Action> actions;
    private final Object sudokuMainLock = new Object();
    private final int threadAmount;
    private final boolean[] isThreadWorking;
    private final Cell[] cells;
    private final RegionManager[] regions;
    private final List<SudokuSolver> solvers;

    public Sudoku(int threadAmount) {
        this.cells = new Cell[81];
        this.regions = new RegionManager[27];
        this.solvers = new ArrayList<>();
        this.actions = new ArrayDeque<>();
        this.attempts = new Stack<>();

        for (int i = 0; i < 81; i++)
            cells[i] = new Cell(i);

        this.threadAmount = threadAmount;

        isThreadWorking = new boolean[threadAmount];
        for (int i = 0; i < threadAmount; i++)
            isThreadWorking[i] = true;

        foundContradiction = false;

        for (int i = 0; i < threadAmount; i++) {
            solvers.add(new SudokuSolver(i, this));
        }

        for (int i = 0; i < 81; i += 9)
            regions[i / 9] = new RowManager(new Cell[] {cells[i], cells[i + 1], cells[i + 2], cells[i + 3],
                    cells[i + 4], cells[i + 5], cells[i + 6], cells[i + 7], cells[i + 8]}, i / 9,this);

        for (int i = 0; i < 9; i++)
            regions[i + 9] = new ColumnManager(new Cell[] {cells[i], cells[i + 9], cells[i + 18], cells[i + 27],
                    cells[i + 36], cells[i + 45], cells[i + 54], cells[i + 63], cells[i + 72]}, i + 9,this);

        for (int i = 0; i < 9; i++) {
            int r = (i / 3) * 3;
            int c = (i % 3) * 3;
            int ind = r * 9 + c;
            regions[i + 18] = new BoxManager(new Cell[] {cells[ind], cells[ind + 1], cells[ind + 2], cells[ind + 9],
                    cells[ind + 10], cells[ind + 11], cells[ind + 18], cells[ind + 19], cells[ind + 20]}, i + 18,this);
        }

        for (int j = 0; j < 27; j++)
            solvers.get(j % threadAmount).assignData(regions[j]);
    }

    public int activeThreadAmount() {
        int result = 0;

        for (int i = 0; i < threadAmount; i++)
            if (isThreadWorking[i])
                result++;

        return result;
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

    public void resumeAllThreads(int threadID) {

        if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
            System.out.println("LOCK\tThread " + threadID + "\tattempted to take SudokuMainMutex (for resumeAllThreads)");

        synchronized (sudokuMainLock) {

            if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
                System.out.println("LOCK\tThread " + threadID + "\ttook SudokuMainMutex (for resumeAllThreads)");

            for (int i = 0; i < threadAmount; i++)
                if (!isThreadWorking[i]) {

                    if (SudokuHandler.LOG_THREAD_ACTIONS)
                        System.out.println("ACTION\tThread " + threadID + "\tSending wake-up signal to thread " + i);

                    isThreadWorking[i] = true;
                    solvers.get(i).signalToWork(threadID);
                }
        }

        if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
            System.out.println("LOCK\tThread " + threadID + "\treleased SudokuMainMutex (for resumeAllThreads)");
    }

    public boolean threadStopAttempt(int threadID) {

        // Assume that it is not the last thread (if it is the last thread we will not stop it)
        boolean shouldThreadBeStopped = true;

        if (SudokuHandler.LOG_THREAD_ACTIONS)
            System.out.println("ACTION\tThread " + threadID + "\tAttempts to sleep");

        if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
            System.out.println("LOCK\tThread " + threadID + "\tattempted to take SudokuMainMutex (for threadStopAttempt)");

        synchronized (sudokuMainLock) {

            if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
                System.out.println("LOCK\tThread " + threadID + "\ttook SudokuMainMutex (for threadStopAttempt)");

            isThreadWorking[threadID] = false;
            int activeThreads = activeThreadAmount();
            //System.out.println("Thread " + threadID + " stop attempt (Thread amount = " + activeThreads + ")");
            if (activeThreads == 0) {

                if (SudokuHandler.LOG_THREAD_ACTIONS)
                    System.out.println("ACTION\tThread " + threadID + "\tWill not sleep as it is the last working thread now");

                isThreadWorking[threadID] = true;

                /*
                    Case (1) Contradiction: If there is a contradiction, check if there is any non-contradicting state
                    in the stack. If so restore the state and wake up all solvers. Otherwise we conclude that the puzzle
                    is not solvable. In this case, just kill the threads
                */
                if (foundContradiction) {

                    if (SudokuHandler.LOG_THREAD_ACTIONS)
                        System.out.println("ACTION\tThread " + threadID + "\tHandling the contradiction");

                    // If there is no non-contradicting state in the stack, kill the threads...
                    if (attempts.empty()) {

                        if (SudokuHandler.LOG_THREAD_ACTIONS)
                            System.out.println("ACTION\tThread " + threadID + "\tNo state to restore, could not solve the puzzle, will send kill signal to all threads");

                        for (int i = 0; i < threadAmount; i++)
                            solvers.get(i).setKillFlag();

                        resumeAllThreads(threadID);
                    }

                    // If there is a state, return to that state and wake up all threads...
                    else {
                        // Restore

                        if (SudokuHandler.LOG_THREAD_ACTIONS)
                            System.out.println("ACTION\tThread " + threadID + "\tRestoring to the latest non-contradicting state on the stack");

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

                        cells[attempts.peek().changedCell].remove(attempts.peek().pickedValue, threadID);
                        addToQueue(action, threadID);
                        attempts.pop();

                        // Signal all threads to run again
                        resumeAllThreads(threadID);
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
                        System.out.println("Thread " + threadID + " : Puzzle is solved!");
                        for (int i = 0; i < threadAmount; i++)
                            solvers.get(i).setKillFlag();

                        resumeAllThreads(threadID);

                    }
                    // Case (2) : We will make a guess
                    else {
                        //System.out.println("Thread " + threadID + ": We have to make a guess!");

                        SudokuBackup backup = new SudokuBackup();
                        for (int i = 0; i < 81; i++)
                            backup.values[i] = cells[i].getCandidates();

                        backup.pickedValue = cells[isNotFinished].pickCandidate();
                        backup.changedCell = isNotFinished;
                        Action action = new Action(isNotFinished, 0, EliminationEvent.TRYING, backup.pickedValue);
                        addToQueue(action, threadID);
                        attempts.push(backup);

                        // Run the threads again
                        resumeAllThreads(threadID);
                    }
                }

                shouldThreadBeStopped = false;
            }
        }

        if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
            System.out.println("LOCK\tThread " + threadID + "\treleased SudokuMainMutex (for threadStopAttempt)");

        return shouldThreadBeStopped;
    }

    public void addToQueue(Action action, int threadID) {

        if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
            System.out.println("LOCK\tThread " + threadID + " attempted to take queueLock" + threadID);

        synchronized(actions) {

            if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
                System.out.println("LOCK\tThread " + threadID + " took queueLock" + threadID);

            actions.add(action);
        }

        if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
            System.out.println("LOCK\tThread " + threadID + " released queueLock" + threadID);
    }

    public Report getReport() {

        Report report = new Report();

        report.timeElapsed = 0L;

        boolean contradiction = false;

        while (actions.size() != 0) {

            if (contradiction) {
                while (actions.size() > 0 && actions.peek().getEvent() != EliminationEvent.ROLLBACK)
                    actions.remove();
                contradiction = false;
                continue;
            }

            if (actions.peek().getEvent() == EliminationEvent.CONTRADICTION) {
                contradiction = true;
            }

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