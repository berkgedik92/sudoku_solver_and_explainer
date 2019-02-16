package application.sudoku_solver.Solver.Managers;

import application.sudoku_solver.Solver.Models.Cell;
import application.sudoku_solver.Solver.Models.CellOperations;
import application.sudoku_solver.Solver.Reporter.Action;
import application.sudoku_solver.Solver.Models.Sudoku;
import application.sudoku_solver.Solver.Reporter.EliminationEvent;

public class RowManager extends RegionManager {

    private final int rowIndex;
    private final int firstBoxIndex;
    private final int secondBoxIndex;
    private final int thirdBoxIndex;

    public RowManager(Cell cell1, Cell cell2, Cell cell3,
                       Cell cell4, Cell cell5, Cell cell6,
                       Cell cell7, Cell cell8, Cell cell9,
                       int regionID, Sudoku puzzle) {

        super(cell1, cell2, cell3, cell4, cell5, cell6, cell7, cell8, cell9, regionID, puzzle);

        this.rowIndex = regionID;
        this.firstBoxIndex = 18 + ((rowIndex / 3) * 3);
        this.secondBoxIndex = firstBoxIndex + 1;
        this.thirdBoxIndex = firstBoxIndex + 2;

    }

    @Override
    public boolean specialAction() {
        boolean change = false;

        /*
            The row is included in three boxes. For each number, we want to detect in which boxes the number
            appears as a candidate.

            candidates[X][Y] = In how many cells (where a cell belongs to both this row and Yth box)
                               the number X appears as a candidate
         */
        int[][] candidates = new int[9][3];

        for (int i = 0; i < 9; i++) {
            for (char ch = '1'; ch <= '9'; ch++)
                if (CellOperations.isCovered(getInt(ch + ""), cellValues[i]))
                    candidates[ch - '1'][i / 3]++;
        }

        /*
            Find a candidate which appears only in a certain box. If this can be done, we will conclude that
            this candidate cannot appear in other cells of that box.
         */
        for (int i = 0; i < 9; i++)
            if (candidates[i][0] >= 2 && candidates[i][1] == 0 && candidates[i][2] == 0)
                change = specialElimination(firstBoxIndex, getEliminatedCandidates(i)) || change;

            else if (candidates[i][1] >= 2 && candidates[i][0] == 0 && candidates[i][2] == 0)
                change = specialElimination(secondBoxIndex, getEliminatedCandidates(i)) || change;

            else if (candidates[i][2] >= 2 && candidates[i][0] == 0 && candidates[i][1] == 0)
                change = specialElimination(thirdBoxIndex, getEliminatedCandidates(i)) || change;

        return change;
     }

    @Override
    public boolean specialElimination(int targetID, int eliminatedCandidates) {
        //At the end this will be bigger than 0 if this eliminated attempt could really eliminate a candidate in a cell
        int change = 0;

        Action action = new Action(regionID, targetID, EliminationEvent.POINTING_PAIR, eliminatedCandidates);
        Cell[] boxCells = mySudoku.getCells(targetID);

        /*
            In the region whose ID is equal to "targetID", in which cells we are trying to eliminate candidates?
            We keep their indices here (for this elimination, those are the cells which are not located in this
            column and located in the box region whose ID = targetID)
         */
        int[] targetCells = new int[6];
        int cursor = 0;

        for (int i = 0; i < 9; i++)
            if (boxCells[i].getRowIndex() != rowIndex)
                targetCells[cursor++] = i;
		    else if (CellOperations.isCovered(eliminatedCandidates, boxCells[i].getCandidates()))
                action.greenCells[i] = eliminatedCandidates;

        for (int i = 0; i < 6; i++) {
            action.redCells[targetCells[i]] = boxCells[targetCells[i]].remove(eliminatedCandidates);
            change += action.redCells[targetCells[i]];
        }

        //If we could eliminate some candidates, then add this action to our action list.
        if (change > 0)
            mySudoku.addToQueue(action);

        return (change > 0);
     }
}