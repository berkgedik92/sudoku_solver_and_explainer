package application.sudoku_solver.Solver.Models;

import application.sudoku_solver.Solver.SudokuHandler;

public class Cell {

    public static final int allValues = 511;

    private final Object lock = new Object();
    private final int index;
    public int data;

    public Cell (int index) {
        this.index = index;
        this.data = Cell.allValues;
    }

    /*
        Picks one candidate among possible candidates and assigns this
        one candidate as all possible candidates
     */
    public int pickCandidate() {
        int number = 1;
        for (; number <= 256; number = number << 1) {
            if ((data & number) > 0) {
                data = number;
                break;
            }
        }
        return number;
    }

    public void assign(char number) {
        int temp = 1;
        data = temp << (number - '1');
    }

    public void assign(int data) {
        this.data = data;
    }

    /*
        Removes "value" from data, returns what
        has been removed
     */
    public int remove(int value, int threadID) {
        return intersect(~value, threadID);
    }

    /*
        Data will be equal to the intersection of data and value
        It returns (data - intersection = what have been eliminated)
     */
    public int intersect(int value, int threadID) {
        int eliminated;

        if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
            System.out.println("LOCK\tThread " + threadID + "\tattempted to take Cell" + index + "Lock");

        synchronized (lock) {
            if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
                System.out.println("LOCK\tThread " + threadID + "\ttook Cell" + index + "Lock");

            int oldData = data;
            data = data & value;
            eliminated = oldData & (~data);
        }

        if (SudokuHandler.LOG_LOCK_ACQUIREMENTS)
            System.out.println("LOCK\tThread " + threadID + "\treleased Cell" + index + "Lock");

        return eliminated;
    }

    public int getCandidates() {
        return data;
    }

    public int getColumnIndex() {
        return index % 9;
    }

    public int getRowIndex() {
        return index / 9;
    }

    public int getBoxIndex() {
        return (((index / 9) / 3) * 3) + ((index % 9) / 3);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        int mask = 1;
        for (int i = 0; i < 9; i++, mask = mask << 1)
            if ((mask & data) >= 1)
                result.append(Character.toString((char) ('1' + i)));
        return result.toString();
    }
}
