package application.sudoku_solver.Solver.Managers;

import application.sudoku_solver.Solver.Models.Cell;
import application.sudoku_solver.Solver.Models.CellOperations;
import application.sudoku_solver.Solver.Reporter.Action;
import application.sudoku_solver.Solver.Models.Sudoku;
import application.sudoku_solver.Solver.Reporter.EliminationEvent;

public class BoxManager extends RegionManager {

    private final int boxIndex;
    private final int firstRowIndex;
    private final int secondRowIndex;
    private final int thirdRowIndex;
    private final int firstColumnIndex;
    private final int secondColumnIndex;
    private final int thirdColumnIndex;

    public BoxManager(Cell[] cells, int regionID, Sudoku puzzle) {

        super(cells, regionID, puzzle);

        this.boxIndex = regionID - 18;
        this.firstRowIndex = (boxIndex / 3) * 3;
        this.secondRowIndex = firstRowIndex + 1;
        this.thirdRowIndex = firstRowIndex + 2;
        this.firstColumnIndex = 9 + (boxIndex % 3) * 3;
        this.secondColumnIndex = firstColumnIndex + 1;
        this.thirdColumnIndex = firstColumnIndex + 2;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean specialAction() {
        boolean change = false;

        /*
            The box is included in three rows and three columns. For each number, we want to detect in which
            rows and columns the number appears as a candidate.

            candidatesForRows[X][Y] = In how many cells (where a cell belongs to both this box and Yth row)
                                      the number X appears as a candidate

            candidatesForColumns[X][Y] = In how many cells (where a cell belongs to both this box and Yth column)
                                         the number X appears as a candidate
         */
        int[][] candidatesForRows = new int[9][3];
        int[][] candidatesForColumns = new int[9][3];

        for (int i = 0; i < 9; i++) {
            for (char ch = '1'; ch <= '9'; ch++)
                if (CellOperations.isCovered(getInt(ch + ""), cellValues[i])) {
                    candidatesForRows[ch - '1'][i / 3]++;
                    candidatesForColumns[ch - '1'][i % 3]++;
            }
        }

        for (int i = 0; i < 9; i++) {
            /*
                Find a candidate which appears only in a certain row. If this can be done, we will conclude that
                this candidate cannot appear in other cells of that row.
            */
            if (candidatesForRows[i][0] >= 2 && candidatesForRows[i][1] == 0 && candidatesForRows[i][2] == 0)
                change = specialElimination(firstRowIndex, getEliminatedCandidates(i)) || change;

            else if (candidatesForRows[i][1] >= 2 && candidatesForRows[i][0] == 0 && candidatesForRows[i][2] == 0)
                change = specialElimination(secondRowIndex, getEliminatedCandidates(i)) || change;

            else if (candidatesForRows[i][2] >= 2 && candidatesForRows[i][0] == 0 && candidatesForRows[i][1] == 0)
                change = specialElimination(thirdRowIndex, getEliminatedCandidates(i)) || change;

            /*
                Find a candidate which appears only in a certain column. If this can be done, we will conclude that
                this candidate cannot appear in other cells of that column.
            */
            if (candidatesForColumns[i][0] >= 2 && candidatesForColumns[i][1] == 0 && candidatesForColumns[i][2] == 0)
                change = specialElimination(firstColumnIndex, getEliminatedCandidates(i)) || change;

            else if (candidatesForColumns[i][1] >= 2 && candidatesForColumns[i][0] == 0 && candidatesForColumns[i][2] == 0)
                change = specialElimination(secondColumnIndex, getEliminatedCandidates(i)) || change;

            else if (candidatesForColumns[i][2] >= 2 && candidatesForColumns[i][0] == 0 && candidatesForColumns[i][1] == 0)
                change = specialElimination(thirdColumnIndex, getEliminatedCandidates(i)) || change;
        }

        return change;
    }

    @Override
    public boolean specialElimination(int targetID, int eliminatedCandidates) {
        //At the end this will be bigger than 0 if this eliminated attempt could really eliminate a candidate in a cell
        int change = 0;

        Action action = new Action(regionID, targetID, EliminationEvent.BOX_REDUCTION, eliminatedCandidates);
        Cell[] otherCells = mySudoku.getCells(targetID);

        /*
            In the region whose ID is equal to "targetID", in which cells we are trying to eliminate candidates?
            We keep their indices here (for this elimination, those are the cells which are not located in this
            box and located in the region whose ID = targetID)
         */
        int[] targetCells = new int[6];
        int cursor = 0;

        for (int i = 0; i < 9; i++)
            if (otherCells[i].getBoxIndex() != boxIndex)
                targetCells[cursor++] = i;
		    else if (CellOperations.isCovered(eliminatedCandidates, otherCells[i].getCandidates()))
                action.greenCells[i] = eliminatedCandidates;

        for (int i = 0; i < 6; i++) {
            action.redCells[targetCells[i]] = otherCells[targetCells[i]].remove(eliminatedCandidates);
            change += action.redCells[targetCells[i]];
        }

        //If we could eliminate some candidates, then add this action to our action list.
        if (change > 0)
            mySudoku.addToQueue(action);

        return (change > 0);
    }
}



