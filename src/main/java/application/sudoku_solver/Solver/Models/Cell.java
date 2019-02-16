package application.sudoku_solver.Solver.Models;

public class Cell {

    public static final int allValues = 511;

    private final Object lock = new Object();
    private final int index;
    public int data;

    public Cell (int index) {
        synchronized (lock) {
            this.index = index;
            this.data = Cell.allValues;
        }
    }

    public void reset() {
        synchronized (lock) {
            this.data = Cell.allValues;
        }
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
        synchronized (lock) {
            int temp = 1;
            data = temp << (number - '1');
        }
    }

    public void assign(int data) {
        synchronized (lock) {
            this.data = data;
        }
    }

    /*
        Removes "value" from data, returns what
        has been removed
     */
    public int remove(int value) {
        return intersect(~value);
    }

    /*
        Data will be equal to the intersection of data and value
        It returns (data - intersection = what have been eliminated)
     */
    public int intersect(int value) {
        int eliminated;
        synchronized (lock) {
            int oldData = data;
            data = data & value;
            eliminated = oldData & (~data);
        }
        return eliminated;
    }

    public int getCandidates() {
        int temp;
        synchronized (lock) {
            temp = data;
        }
        return temp;
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
