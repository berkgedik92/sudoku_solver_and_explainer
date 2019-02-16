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
        System.out.println("Thread " + threadID + " stared!");
        while (isLiving) {
            boolean isThereNewData = false;
            boolean anyCandidateEliminated = false;

            /*
                Check if there are new data (i.e the candidates for any cell that this solver is responsible
                from have been changed. If there is new data, just fetch the new data and keep doing so until
                no new data comes. When new data stops coming then the solver can try to eliminate further
                candidates at cells that the solver is responsible from.
             */

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
            if (!isThereNewData) {
                System.out.println("Thread " + threadID + " tries to eliminate candidates!");
                for (int i = 0; i < regions.size(); i++) {
                    for (int j = 0; j < 9; j++)
                        regions.get(i).SudokuCellChanged(j, cellData.get(cellsOfRegions.get(i).get(j)));
                    anyCandidateEliminated = regions.get(i).NakedElimination() || anyCandidateEliminated;
                }

                if (!anyCandidateEliminated)
                    for (RegionManager region : regions)
                        anyCandidateEliminated = region.specialAction() || anyCandidateEliminated;

                if (!anyCandidateEliminated)
                    for (RegionManager region : regions)
                        anyCandidateEliminated = region.HiddenElimination() || anyCandidateEliminated;
            }

            /*
                If some solver signaled that in the current puzzle state there is a contradiction then
                we must stop working and after all solvers stop working we need to rollback to the latest
                non-contradicting state. Only after that solvers can start working again.
            */
            if (shouldStopDueToContradiction && puzzle.threadStopAttempt()) {
                while (shouldStopDueToContradiction) {
                    synchronized (lock) {
                        try {
                            System.out.println("Thread " + threadID + " will sleep (contradiction)!");
                            lock.wait();
                        } catch (InterruptedException e) {
                            System.out.println("Thread " + threadID + " is interrupted, thread will stop...");
                            isLiving = false;
                            break;
                        }
                    }
                }
                System.out.println("Thread " + threadID + " wake up (contradiction)!");
            }

            else if (!isThereNewData && !anyCandidateEliminated && puzzle.threadStopAttempt()) {
                synchronized (lock) {
                    try {
                        System.out.println("Thread " + threadID + " is sleeping (no candidate elimination)!");
                        lock.wait();
                        System.out.println("Thread " + threadID + " wake up (no candidate elimination)!");
                    } catch (InterruptedException e) {
                        System.out.println("Thread " + threadID + " is interrupted, thread will stop...");
                        isLiving = false;
                        break;
                    }
                }
            }

            if (anyCandidateEliminated) {
                System.out.println("Thread " + threadID + " eliminated something and signaled all threads to work!");
                puzzle.continueWork();
            }
        }

        System.out.println("Thread " + threadID + " quitted!");
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

    public void signalToWork() {
        shouldStopDueToContradiction = false;
        synchronized (lock) {
            lock.notify();
        }
    }

    public void signalToStopDueToContradiction() {
        shouldStopDueToContradiction = true;
    }

    public void killThread() {
        isLiving = false;
        shouldStopDueToContradiction = false;
        synchronized (lock) {
            lock.notify();
        }
    }
}
