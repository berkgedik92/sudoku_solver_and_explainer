package application.sudoku_solver.Solver.Reporter;

import lombok.Getter;

@Getter
public class Action {

    private final int sourceID;
    private final int targetID;
    private final EliminationEvent event;
    private final int relatedCandidates;
    public int[] greenCells;
    public int[] redCells;
    public int[] values;

    public Action (int sourceID, int targetID, EliminationEvent event, int relatedCandidates) {

        this.sourceID = sourceID;
        this.targetID = targetID;
        this.event = event;
        this.relatedCandidates = relatedCandidates;

        if (event == EliminationEvent.ROLLBACK)
            values = new int[81];

        if (event == EliminationEvent.HIDDEN_ELIMINATION ||
                event == EliminationEvent.BOX_REDUCTION ||
                event == EliminationEvent.POINTING_PAIR ||
                event == EliminationEvent.NAKED_ELIMINATION) {

            redCells = new int[9];

            if (event != EliminationEvent.HIDDEN_ELIMINATION)
                greenCells = new int[9];
        }
    }
}
