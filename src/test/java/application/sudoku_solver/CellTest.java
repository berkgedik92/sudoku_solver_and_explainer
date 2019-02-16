package application.sudoku_solver;

import application.sudoku_solver.Solver.Models.Cell;
import application.sudoku_solver.Solver.Models.CellOperations;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CellTest {

    private int getInt (String data) {
        int result = 0;
        int mask = 1;
        for (int i = 0; i < data.length(); i++)
            result +=  mask << (data.charAt(i) - '1');
        return result;
    }


    @Test
    public void createCellToString() {
        Cell cell = new Cell(80);
        String s = cell.toString();
        assertEquals(s, "123456789");
    }

    @Test
    public void stringToInt() {
        int i = getInt("123456789");
        assertEquals(i, 511);
    }

    @Test
    public void assignNumTest() {
        Cell cell = new Cell(80);
        cell.assign('7');
        String s = cell.toString();
        assertEquals(s, "7");
        assertEquals(getInt(s),64);
    }

    @Test
    public void assignTwoNumbers() {
        Cell cell = new Cell(80);
        //Data for 7 and 8
        int data = 64 + 128;
        cell.assign(data);
        String s = cell.toString();
        assertEquals(s, "78");
        assertEquals(getInt(s), data);
    }


    @Test
    public void pickCandidateTest() {
        Cell cell = new Cell(80);
        //Data for 7 and 8
        int data = 64 + 128;
        cell.assign(data);
        cell.pickCandidate();
        String s = cell.toString();
        assertEquals(s, "7");
    }

    @Test
    public void ResetTest() {
        Cell cell = new Cell(80);
        //Data for 7 and 8
        int data = 64 + 128;
        cell.assign(data);
        cell.reset();
        String s = cell.toString();
        assertEquals(s, "123456789");
    }

    @Test
    public void intersectionTest() {
        Cell cell1 = new Cell(80);
        Cell cell2 = new Cell(80);
        int data1 = 8 + 32 + 64 + 128;
        int data2 = 1 + 32 + 64 + 256;
        cell1.assign(data1);
        cell2.assign(data2);
        int eliminated = cell1.intersect(getInt(cell2.toString()));
        assertEquals(2, CellOperations.getCandidateAmount(cell1.data));
        assertEquals(eliminated, 8 + 128);
    }

    @Test
    public void extractTest() {
        Cell cell1 = new Cell(80);
        Cell cell2 = new Cell(80);
        //12345
        int data1 = 1 + 2 + 4 + 8 + 16;

        //239
        int data2 = 2 + 4 + 256;

        cell1.assign(data1);
        cell2.assign(data2);

        int r = cell1.remove(cell2.getCandidates());
        assertEquals(cell1.toString(), "145");
        assertEquals(r, 2 + 4);
    }

    @Test
    public void haveSthCommonTest1() {
        Cell cell1 = new Cell(80);
        Cell cell2 = new Cell(80);
        Cell cell3 = new Cell(80);
        //12345
        int data1 = 1 + 2 + 4 + 8 + 16;

        //239
        int data2 = 2 + 4 + 256;

        //78
        int data3 = 64 + 128;
        cell1.assign(data1);
        cell2.assign(data2);
        cell3.assign(data3);

        assertTrue(CellOperations.isThereCommonCandidate(cell1.getCandidates(), cell2.getCandidates()));
        assertFalse(CellOperations.isThereCommonCandidate(cell1.getCandidates(), cell3.getCandidates()));
    }

    @Test
    @Ignore
    public void isContainsTest() {
        Cell cell1 = new Cell(80);
        Cell cell2 = new Cell(80);
        Cell cell3 = new Cell(80);
        //12345
        int data1 = 1 + 2 + 4 + 8 + 16;

        //123
        int data2 = 1 + 2 + 4;

        //1239
        int data3 = 1 + 2 + 4 + 256;
        cell1.assign(data1);
        cell2.assign(data2);
        cell3.assign(data3);
    }
}
