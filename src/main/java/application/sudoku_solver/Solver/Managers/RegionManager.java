package application.sudoku_solver.Solver.Managers;

import application.sudoku_solver.Solver.Models.Cell;
import application.sudoku_solver.Solver.Models.CellOperations;
import application.sudoku_solver.Solver.Reporter.Action;
import application.sudoku_solver.Solver.Models.Sudoku;
import application.sudoku_solver.Solver.Reporter.EliminationEvent;

public abstract class RegionManager {

    /*
        Represents all possible candidate lists with one number and two numbers. For example 4 represents the
        candidate list = [3] and 10 represents the candidate list = [2,4].
     */
    private static final int[] allSubsets = new int[] {1,2,4,8,16,32,64,128,256,3,5,9,17,33,65,129,257,6,10,18,34,66,130,258,12,20,
            36,68,132,260,24,40,72,136,264,48,80,144,272,96,160,288,192,320,384};

    final int[] cellValues = new int[9];
    final int regionID;

    // We keep references to Cell objects to be able to update their candidate list when we find
    // something to eliminate
    private final Cell[] cells;

    // We keep reference to Sudoku object to be able to inform it about our actions to eliminate a candidate
    // and to inform it in case a contradiction has been detected. Sudoku object keeps the actions to be able
    // to prepare a report for user.
    final Sudoku mySudoku;

    RegionManager(Cell[] cells, int regionID, Sudoku puzzle) {

        this.cells = cells;

        this.regionID = regionID;
        this.mySudoku = puzzle;

        for (int i = 0; i < 9; i++)
            this.cellValues[i] = Cell.allValues;
    }

    public Cell[] getCellRef() {
        return this.cells;
    }

    /*
        When a cell changes, this function will be triggered...
     */
    public void SudokuCellChanged(int cell, int data) {
        cellValues[cell] = data;
    }

    /*
        For more information : http://www.sudokuwiki.org/naked_candidates
     */
    public boolean NakedElimination(int threadID) {
        boolean change = false;
        int[] candidates = new int[9];
        int[] amounts = new int[9];
        candidates[0] = Cell.allValues;

        for (int i = 0; i < 9; i++) {
            int place = -1;
            for (int j = 0; j < 9; j++) {
                if (candidates[j] == cellValues[i]) {
                    place = j;
                    break;
                }
                else if (amounts[j] == 0)
                    place = j;
            }
            amounts[place]++;
            candidates[place] = cellValues[i];
        }

        for (int i = 0; i < 9; i++) {
            if (amounts[i] > 0) {
                int length = CellOperations.getCandidateAmount(candidates[i]);
                Action action = new Action(regionID, regionID, EliminationEvent.NAKED_ELIMINATION, candidates[i]);
                if (length == amounts[i]) {
                    int changeAmount = 0;

                    for (int cell = 0; cell < 9; cell++) {
                        if (cellValues[cell] != candidates[i]) {
                            action.redCells[cell] = cells[cell].remove(candidates[i], threadID);
                            changeAmount += action.redCells[cell];
                        }
                        else {
                            action.greenCells[cell] = candidates[i];
                        }
                    }
                    if (changeAmount > 0) {
                        mySudoku.addToQueue(action, threadID);
                        change = true;
                    }
                }
            }
        }
        return change;
    }

    /*
        For more information : http://www.sudokuwiki.org/Hidden_Candidates
    */
    public boolean HiddenElimination (int threadID) {
        for (int allSubset : allSubsets) {
            Action action = new Action(regionID, regionID, EliminationEvent.HIDDEN_ELIMINATION, allSubset);
            boolean[] cellsToEliminate = new boolean[9];
            int candidateCount = 0;
            int eliminateCount = 0;
            int candidateLength = CellOperations.getCandidateAmount(allSubset);

            for (int j = 0; j < 9; j++) {
                if (CellOperations.isThereCommonCandidate(cellValues[j], allSubset)) {
                    candidateCount++;
                    if (CellOperations.isCovered(allSubset, cellValues[j])) {
                        cellsToEliminate[j] = true;
                        eliminateCount++;
                    } else
                        cellsToEliminate[j] = false;
                } else
                    cellsToEliminate[j] = false;
            }

            if ((candidateLength == candidateCount) && (eliminateCount > 0)) {
                int changeData = 0;
                for (int j = 0; j < 9; j++) {
                    if (cellsToEliminate[j]) {
                        action.redCells[j] = cells[j].intersect(allSubset, threadID);
                        changeData += action.redCells[j];
                    }
                }
                if (changeData > 0) {
                    mySudoku.addToQueue(action, threadID);
                    return true;
                }
            } else if (candidateLength > candidateCount) {
                Action contradiction = new Action(regionID, 0, EliminationEvent.CONTRADICTION, allSubset);
                mySudoku.addToQueue(contradiction, threadID);
                mySudoku.signalContradiction();
            }
        }

        return false;
    }

    int getInt(String data) {
        int result = 0;
        int mask = 1;
        for (int i = 0; i < data.length(); i++)
            result +=  mask << (data.charAt(i) - '1');
        return result;
    }

    int getEliminatedCandidates(int eliminatedCandidateIndex) {
        char ch = (char)('1' + eliminatedCandidateIndex);
        String eliminate = Character.toString(ch);
        return getInt(eliminate);
    }

    /*
        Pointing pair or getBoxIndex reduction. For more information:
        http://www.sudokuwiki.org/intersection_removal
    */
    public abstract boolean specialAction(int threadID);

    /*
        When a RegionManager object (with ID = sourceID) will find a candidate to eliminate in its specialAction
        function, it will signal it by calling this function. Then another RegionManager object (with ID = targetID)
        will perform the necessary checks.
     */
    public abstract boolean specialElimination(int targetID, int eliminatedCandidates, int threadID);
}
