package application.sudoku_solver.Solver;

import application.sudoku_solver.Solver.Managers.RegionManager;
import application.sudoku_solver.Solver.Models.Cell;
import application.sudoku_solver.Solver.Models.Sudoku;
import java.util.ArrayList;
import java.util.List;

public class SudokuSolver extends Thread {

    private boolean shouldStopDueToContradiction;
    private boolean isLiving;
    private final Object lock = new Object();
    private final int threadID;
    private boolean isThereNewData = false;
    private final Sudoku puzzle;
    private final List<RegionManager> regions;
    private final List<Cell> cells;
    private final List<Integer> cellData;
    private final List<List<Integer>> cellsOfRegions;

    public SudokuSolver(int ID, Sudoku puzzle) {
        this.threadID = ID;
        this.puzzle = puzzle;
        this.shouldStopDueToContradiction = false;
        this.isLiving = true;
        this.regions = new ArrayList<>();
        this.cells = new ArrayList<>();
        this.cellData = new ArrayList<>();
        this.cellsOfRegions = new ArrayList<>();
    }

    @Override
    public void run() {
        //System.out.println("Thread " + threadID + " stared!");
        while (isLiving) {
            isThereNewData = false;
            boolean anyCandidateEliminated = false;

            /*
                Check if there are new data (i.e the candidates for any cell that this solver is responsible
                from have been changed. If there is new data, just fetch the new data and keep doing so until
                no new data comes. When new data stops coming then the solver can try to eliminate further
                candidates at cells that the solver is responsible from.
             */

            if (SudokuHandler.LOG_THREAD_ACTIONS)
                System.out.println("ACTION\tThread " + threadID + "\tFetching latest candidate lists for its cells...");

            for (int i = 0; i < cells.size(); i++) {
                int candidates = cells.get(i).getCandidates();
                if (candidates != cellData.get(i)) {
                    isThereNewData = true;
                    cellData.set(i, candidates);
                }
            }

            /*
                If there is no new data, try to eliminate some candidates by using elimination logic.
                Keep track of whether any candidate could be eliminated or not.
            */
            if (isLiving && !isThereNewData) {

                for (int i = 0; i < regions.size(); i++) {
                    for (int j = 0; j < 9; j++)
                        regions.get(i).SudokuCellChanged(j, cellData.get(cellsOfRegions.get(i).get(j)));
                    anyCandidateEliminated = regions.get(i).NakedElimination(threadID) || anyCandidateEliminated;

                    if (SudokuHandler.LOG_THREAD_ACTIONS) {
                        if (anyCandidateEliminated)
                            System.out.println("ACTION\tThread " + threadID + "\tEliminated some candidates by NakedElimination method");
                        else
                            System.out.println("ACTION\tThread " + threadID + "\tFailed to eliminated some candidates by NakedElimination method");
                    }
                }

                if (!anyCandidateEliminated) {
                    for (RegionManager region : regions)
                        anyCandidateEliminated = region.specialAction(threadID) || anyCandidateEliminated;

                    if (SudokuHandler.LOG_THREAD_ACTIONS) {
                        if (anyCandidateEliminated)
                            System.out.println("ACTION\tThread " + threadID + "\tEliminated some candidates by specialAction method");
                        else
                            System.out.println("ACTION\tThread " + threadID + "\tFailed to eliminated some candidates by specialAction method");
                    }
                }

                if (!anyCandidateEliminated) {
                    for (RegionManager region : regions)
                        anyCandidateEliminated = region.HiddenElimination(threadID) || anyCandidateEliminated;

                    if (SudokuHandler.LOG_THREAD_ACTIONS) {
                        if (anyCandidateEliminated)
                            System.out.println("ACTION\tThread " + threadID + "\tEliminated some candidates by HiddenElimination method");
                        else
                            System.out.println("ACTION\tThread " + threadID + "\tFailed to eliminated some candidates by HiddenElimination method");
                    }
                }
            }

            /*
                If some solver signaled that in the current puzzle state there is a contradiction then
                we must stop working and after all solvers stop working we need to rollback to the latest
                non-contradicting state. Only after that solvers can start working again.
            */

            if (isLiving && shouldStopDueToContradiction && puzzle.threadStopAttempt(threadID)) {

                if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
                    System.out.println("LOCK\tThread " + threadID + "\tattempted to take lock of SudokuSolver" + threadID + "Lock (for Contradiction)");

                synchronized (lock) {
                    if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
                        System.out.println("LOCK\tThread " + threadID + "\ttook lock of SudokuSolver" + threadID + "Lock (for Contradiction)");

                    if (SudokuHandler.LOG_THREAD_ACTIONS)
                        System.out.println("ACTION\tThread " + threadID + "\tWill sleep until contradiction is fixed...");

                    while (shouldStopDueToContradiction) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            System.out.println("Thread " + threadID + " is interrupted, thread will stop...");
                            isLiving = false;
                            break;
                        }
                    }

                    if (SudokuHandler.LOG_THREAD_ACTIONS)
                        System.out.println("ACTION\tThread " + threadID + "\tWoke up (slept due to contradiction before)");
                }
            }

            if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
                System.out.println("LOCK\tThread " + threadID + "\treleased lock of SudokuSolver" + threadID + "Lock (for Contradiction)");

            if (isLiving && !isThereNewData && !anyCandidateEliminated && puzzle.threadStopAttempt(threadID)) {

                if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
                    System.out.println("LOCK\tThread " + threadID + "\tattempted to take lock of SudokuSolver" + threadID + "Lock (for Elimination)");

                synchronized (lock) {

                    if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
                        System.out.println("LOCK\tThread " + threadID + "\ttook lock of SudokuSolver" + threadID + "Lock (for Elimination)");

                    if (SudokuHandler.LOG_THREAD_ACTIONS)
                        System.out.println("ACTION\tThread " + threadID + "\tWill sleep until new candidate list comes");

                    while (!isThereNewData) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            System.out.println("Thread " + threadID + " is interrupted, thread will stop...");
                            isLiving = false;
                            break;
                        }
                    }

                    if (SudokuHandler.LOG_THREAD_ACTIONS)
                        System.out.println("ACTION\tThread " + threadID + "\tWoke up (slept to wait for new data before)");
                }
            }

            if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
                System.out.println("LOCK\tThread " + threadID + "\treleased lock of SudokuSolver" + threadID + "Lock (for Elimination)");

            if (anyCandidateEliminated) {

                if (SudokuHandler.LOG_THREAD_ACTIONS)
                    System.out.println("ACTION\tThread " + threadID + "\tEliminated some candidates. It will wake up sleeping threads now");

                puzzle.resumeAllThreads(threadID);
            }
        }

        if (SudokuHandler.LOG_THREAD_ACTIONS)
            System.out.println("ACTION\tThread " + threadID + "\tQuit");
    }

    public void assignData(RegionManager regions) {
        this.regions.add(regions);
        Cell[] cellRef = regions.getCellRef();
        List<Integer> temp = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            int place = -1;
            for (int j = 0; j < cells.size(); j++) {
                if (cells.get(j) == cellRef[i]) {
                    place = j;
                    break;
                }
            }
            if (place == -1) {
                place = cells.size();
                cells.add(cellRef[i]);
                cellData.add(Cell.allValues);
            }
            temp.add(place);
        }
        cellsOfRegions.add(temp);
    }

    public void signalToWork(int threadID) {
        shouldStopDueToContradiction = false;
        isThereNewData = true;

        if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
            System.out.println("LOCK\tThread " + threadID + "\tattempted to take lock of SudokuSolver" + this.threadID + "Lock (for signalToWork)");

        synchronized (lock) {

            if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
                System.out.println("LOCK\tThread " + threadID + "\ttook lock of SudokuSolver" + this.threadID + "Lock (for signalToWork)");

            lock.notify();
        }

        if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
            System.out.println("LOCK\tThread " + threadID + "\treleased lock of SudokuSolver" + this.threadID + "Lock (for signalToWork)");
    }

    public void signalToStopDueToContradiction() {
        shouldStopDueToContradiction = true;
    }

    public void setKillFlag() {
        isLiving = false;
    }
}
