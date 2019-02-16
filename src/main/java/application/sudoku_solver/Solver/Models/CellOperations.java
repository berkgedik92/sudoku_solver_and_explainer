package application.sudoku_solver.Solver.Models;

public class CellOperations {

    /*
        Return true if data1 and data2 have a common candidate (for instance,
        data1 = 129 (which represents candidates 18) and data2 = 9 (which represents
        candidates 14). data1 and data2 have the candidate 1 in common so the function
        will return true.
     */
    public static boolean isThereCommonCandidate(int data1, int data2) {
        return (data1 & data2) > 0;
    }


    /*
        Returns true if data1 is contained by data2. For example if data1 = 3
        (which represents candidates 12) and data1 = 131 (which represents candidates 128)
        then the function will return true as 12 is contained by 128.
     */
    public static boolean isCovered(int data1, int data2) {
        return ((data1 & data2) == data1);
    }

    /*
        Returns the number of candidates. For example for data = 131
        (which represents candidates 128) the function will return 3.
     */
    public static int getCandidateAmount(int data) {
        int result = 0;
        int mask = 1;
        for (int i = 0; i < 9; i++, data = data >> 1)
            result += (mask & data);
        return result;
    }

}
