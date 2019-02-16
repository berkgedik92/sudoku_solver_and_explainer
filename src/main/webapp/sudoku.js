const digits = ["1", "2", "3", "4", "5", "6", "7", "8", "9"];

// [1,2,3,4].diff([1,2]) = [3,4]
Array.prototype.diff = function(a) {
    return this.filter(function(i) {return a.indexOf(i) < 0;});
};

function Remove(arr, position) {
    for (let k = position; k < arr.length - 1; k++)
        arr[k] = arr[k + 1];
    arr.pop();
}

/* Cell class */
/*
	position (int) 	= index of the cell (for example, if the cell is in
					  row 1 and col 7, the index is 1*9+7=16)
	value (string) 	= the possible values of the cell. If the value is already given in
				  	  the puzzle, this parameter is a string consisting of one character,
				  	  which is the value of the cell, otherwise it is equal to "123456789"
*/
function Cell (position, values) {

    /* Cell class method headers */
    this.assign = assign;
    this.clean = clean;
    this.removeAllButThem = removeAllButThem;
    this.show = show;
    this.isCandidate = isCandidate;
    this.isOneOfThemCandidate = isOneOfThemCandidate;
    this.getCandidateAmount = getCandidateAmount;
    this.getRowIndex = getRowIndex;
    this.getColIndex = getColIndex;
    this.getBoxIndex = getBoxIndex;
    this.isChanged = isChanged;
    this.resetChangeCounter = resetChangeCounter;

    /* Cell constructor */
    this.changed = false;
    this.position = position;
    this.colIndex = position % 9;
    this.rowIndex = Math.floor(position / 9);
    this.boxIndex = (Math.floor(this.rowIndex / 3) * 3) + Math.floor(this.colIndex / 3);
    this.candidates = [];
    this.numberOfCandidates = -1;
    this.assign(values);

    /* Method implementations */

    /*
        Gets:
            values (string)
        Returns:
            None
        The digits in "values" will be assigned as the candidates for this cell.
        For example, if values = "39", then 3 and 9 are candidates for this cell.
        this.candidates will be [false,false,true,false,false,false,false,false,true]

     */
    function assign(values) {
        for (let number of digits)
            this.candidates[number - 1] = (values.indexOf(number) !== -1);
        this.numberOfCandidates = values.length;
    }

    /*
        Gets:
            values (string)
        Returns:
            None
        The digits in "values" will be removed from candidates list for the cell.
     */
    function clean(values) {
        for (let value of values)
            if (this.candidates[value - 1]) {
                this.candidates[value - 1] = false;
                this.numberOfCandidates--;
                this.changed = true;
            }
    }

    /*
        Gets:
            them (string)
        Returns:
            None
        All candidates except the digits contained in "them" will be removed from the
        candidate list of this cell.
     */

    function removeAllButThem(them) {
        this.clean(digits.diff(them));
    }

    /*
        Returns: (string) all candidates for this cell.
     */
    function show() {

        let temp = "";
        for (let i = 0; i < 9; i++)
            if (this.candidates[i])
                temp += (i+1) + "";
        return temp;
    }

    /*
        Gets:
            value (string) (one digit)
        Returns true if "value" is a candidate. Otherwise, returns false.
     */
    function isCandidate(value) {
        return this.candidates[parseInt(value) - 1];
    }

    /*
        Returns true if at least one of the digits in "values" is among candidates.
        Otherwise, return false.
     */
    function isOneOfThemCandidate(values) {

        for (let i = 0; i < values.length; i++)
            if (this.isCandidate(parseInt(values[i])))
                return true;
        return false;
    }

    /*
        Returns: (int) how many candidates are there for this cell.
     */
    function getCandidateAmount()
    {
        return this.numberOfCandidates;
    }

    function getRowIndex()
    {
        return this.rowIndex;
    }

    function getColIndex()
    {
        return this.colIndex;
    }

    function getBoxIndex()
    {
        return this.boxIndex;
    }

    /*
        Returns: (boolean) true if there is a change in the candidates list since the last time
        resetChangeCounter function is called.
     */
    function isChanged()
    {
        return this.changed;
    }

    function resetChangeCounter()
    {
        this.changed = false;
    }
}

/* Sudoku class */
function Sudoku (input) {

    /* Sudoku method headers */

    this.solve = solve;
    this.guessACell = guessACell;
    this.initialize = initialize;

    this.nakedElimination = nakedElimination;
    this.hiddenElimination = hiddenElimination;
    this.pointingPair = pointingPair;
    this.boxLineReduction = boxLineReduction;

    this.isFinished = isFinished;
    this.isContradicting = isContradicting;
    this.isThereMoreThanOneCandidate = isThereMoreThanOneCandidate;
    this.isChanged = isChanged;

    this.summarizeRegion = summarizeRegion;
    this.areCellsInTheSameRow = areCellsInTheSameRow;
    this.areCellsInTheSameColumn = areCellsInTheSameColumn;
    this.areCellsInTheSameBox = areCellsInTheSameBox;

    this.isThereFrequentCandidateListInAnyRegion = isThereFrequentCandidateListInAnyRegion;
    this.createRegionsData = createRegionsData;

    this.showValues = showValues;

    /* Sudoku constructor */
    this.data = [];

    /* Create cells */
    for (let i = 0; i < 81; i++)
        if (input[i] !== " ")
            this.data.push(new Cell(i, input[i]));
        else
            this.data.push(new Cell(i, "123456789"));

    /*
        Create data structure for each region (Each row, column and box is a region)
        rows[a][b] = Cell object for the "b"th cell in "a"th row.
        ...
        Call this function after "this.data" array is populated.
     */
    function createRegionsData() {
        this.rows = [];
        this.cols = [];
        this.boxes = [];
        this.allRegions = [];
        this.colsAndRows = [];

        for (let i = 0; i < 9; i++)
        {
            this.rows.push([]);
            this.cols.push([]);
            this.boxes.push([]);
        }

        for (let i = 0; i < 9; i++) {
            this.allRegions.push(this.rows[i]);
            this.allRegions.push(this.cols[i]);
            this.allRegions.push(this.boxes[i]);
            this.colsAndRows.push(this.rows[i]);
            this.colsAndRows.push(this.cols[i]);
        }

        for (let i = 0; i < 81; i++) {
            this.rows[this.data[i].getRowIndex()].push(this.data[i]);
            this.cols[this.data[i].getColIndex()].push(this.data[i]);
            this.boxes[this.data[i].getBoxIndex()].push(this.data[i]);
        }
    }

    this.createRegionsData();

    /*
        Resets "change counter" of all cells, so one can check whether there is a change or not
        since the latest call of this function.
     */
    function initialize () {
        for (let cell of this.data)
            cell.resetChangeCounter();
    }

    function showValues() {
        let result = [];
        for (let cell of this.data)
            result.push(cell.show());
        return result;
    }

    /*
        See summarizeRegion function below.
     */
    function CandidatesAndFrequency(candidateList, frequency) {
        this.candidateList = candidateList;
        this.frequency = frequency;

        this.getCandidateList = function() {
            return this.candidateList;
        };

        this.getFrequency = function () {
            return this.frequency;
        };

        this.increaseFrequency = function (amount) {
            this.frequency += amount;
        }
    }

    /*
        Gets:
            region (array of Cells)
        Returns:
            Gets a region and returns a table that summarizes the candidate lists and frequency of each candidate list.
            (list of CandidatesAndFrequency objects)
            For example, let's assume 6 cells have candidate list "123456", 2 cells have candidate list "78" and
            1 cell has candidate list "9".
            In this case the function returns [["123456", 6], ["78", 2], ["9", 1]]
     */
    function summarizeRegion(region) {

        // For now, with cells with candidate lists c1, c2, ..., c9
        // we start with [[c1, 1], [c2, 1], ..., [c9, 1]]
        let result = [];
        for (let cell of region)
            result.push(new CandidatesAndFrequency(cell.show(), 1));

        // Until there are [cX, a] and [cY, b] such that cX = cY and X != Y (assuming the element
        // [cX, a] comes first), remove [cY, b] and replace [cX, a] with [cX, a+b])
        for (let i = 0; i < result.length; i++)
            for (let j = i + 1; j < result.length; j++)
                if (result[i].getCandidateList() === result[j].getCandidateList()) {
                    result[i].increaseFrequency(result[j].getFrequency());
                    Remove(result, j);
                    j--;
                }
        return result;
    }

    /*
        In a region, if there are "A" number of cells which have same candidate list that consists
        of "A" digits, then those digits will be shared among those cells, so other cells cannot have
        those digits as candidates.
        (If there are X cells with same candidate list where the length of candidate list is Y and X < Y, then
        this puzzle would lead to a contradiction and isThereFrequentCandidateListInAnyRegion checks against this case)
     */
    function nakedElimination() {

        for (let region of this.allRegions) {

            // Get unique candidate lists and their frequency in the region
            let regionSummary = this.summarizeRegion(region);

            // If there is a candidate list whose frequency is equal to its length, it will be stored here
            let toRemove = [];

            for (let element of regionSummary)
                if (element.getCandidateList().length ===  element.getFrequency())
                    toRemove.push(element.getCandidateList());

            // For each such candidate list C, find cells in the region whose candidate list is not equal to C
            // For such cells, the digits appearing in C cannot be candidates.
            for (let values of toRemove)
                for (let cell of region)
                    if (cell.show() !== values)
                        cell.clean(values);
        }
    }

    /*
        In a region if "degree" number of digits appear only on "degree" number of cells, those cells can
        only have those digits as candidates. For example, let's assume in a row the digits 1, 2 and 3 are candidates of
        only cells A, B and C. In this case the cells A, B, C can only have the digits 1, 2 and 3 as candidates.
     */
    function hiddenElimination(degree) {

        for (let region of this.allRegions)
        {
            // For this region, detect the digits whose cell is not decided yet.
            let decidedDigits = new Set();
            for (let cell of region)
                if (cell.getCandidateAmount() === 1)
                    decidedDigits.add(cell.show());

            let notDecidedDigits = digits.diff(Array.from(decidedDigits)).join("");

            // For the set of "notDecidedDigits" find all possible subsets with "degree" digits, and
            // store them in "candidateSubsets" together with frequency value. Let's assume notDecidedDigits = "124"
            // and degree = 2. In this case subsets will be "12", "14" and "24". Assuming that among the cells in the region
            // digits 1 and 2 are candidates in exactly 2 cells, in "candidateSubsets" list there will be an element ["12", 2].
            let candidateSubsets = [];
            getSubsets(notDecidedDigits, candidateSubsets, degree, true);
            candidateSubsets = candidateSubsets.map(element => new CandidatesAndFrequency(element, 0));

            for (let subset of candidateSubsets)
                for (let cell of region)
                    if (cell.isOneOfThemCandidate(subset.getCandidateList()))
                        subset.increaseFrequency(1);

            // For elements of "candidateSubsets" where the number of digits is equal to the frequency value,
            // the cells that have at least one of those digits as a candidate cannot have anything different than
            // those digits as candidates.
            for (let subset of candidateSubsets)
                if (subset.getFrequency() === subset.getCandidateList().length)
                    for (let cell of region)
                        if (cell.isOneOfThemCandidate(subset.getCandidateList()))
                            cell.removeAllButThem(subset.getCandidateList());
        }
    }

    /*
        If there are cell(s) in the same box region and sharing the same row or column, if there is a digit which is a candidate
        in only those cells in the box region, then this digit can be removed from candidate list of other cells in the row or column.
     */
    function pointingPair() {

        for (let box of this.boxes) {
            let boxIndex = box[0].getBoxIndex();

            // For this region, detect the digits whose cell is not decided yet.
            let decidedNumbers = new Set();
            for (let cell of box)
                if (cell.getCandidateAmount() === 1)
                    decidedNumbers.add(cell.show());

            let notDecidedNumbers = digits.diff(Array.from(decidedNumbers)).join("");

            // For each such digit, check which cells in the box have it as candidate...
            for (let digit of notDecidedNumbers) {
                let candidates = [];
                for (let cell of box)
                    if (cell.isCandidate(parseInt(digit)))
                        candidates.push(cell);

                // In the box region, if there are cell(s) which have this digit as a
                // candidate and all such cells are in the same row, then the other cells
                // (cells in a different box) in this row cannot have this digit as a candidate.
                if (candidates.length !== 0 && this.areCellsInTheSameRow(candidates))
                    for (let cell of this.rows[candidates[0].getRowIndex()])
                        if (cell.getBoxIndex() !== boxIndex)
                            cell.clean(digit);

                // In the box region, if there are cell(s) which have this digit as a
                // candidate and all such cells are in the same column, then the other cells
                // (cells in a different box) in this column cannot have this digit as a candidate.
                if (candidates.length !== 0 && this.areCellsInTheSameColumn(candidates))
                    for (let cell of this.cols[candidates[0].getColIndex()])
                        if (cell.getBoxIndex() !== boxIndex)
                            cell.clean(digit);
            }
        }
    }

    /*
        If there are cell(s) in the same row or column region and sharing the same box, if there is a digit which is a candidate
        in only those cells in the box region, then this digit can be removed from candidate list of other cells in the box.
     */
    function boxLineReduction() {

        for (let row of this.rows) {
            let rowIndex = row[0].getRowIndex();

            // For this region, detect the digits whose cell is not decided yet.
            let decidedNumbers = new Set();
            for (let cell of row)
                if (cell.getCandidateAmount() === 1)
                    decidedNumbers.add(cell.show());

            let notDecidedNumbers = digits.diff(Array.from(decidedNumbers)).join("");

            // For each such digit, check which cells in the row have it as candidate...
            for (let digit of notDecidedNumbers) {
                let candidates = [];
                for (let cell of row)
                    if (cell.isCandidate(parseInt(digit)))
                        candidates.push(cell);

                // In the row region, if there are cell(s) which have this digit as a
                // candidate and all such cells are in the same box, then the other cells
                // (cells in a different row) in this box cannot have this digit as a candidate.
                if (candidates.length !== 0 && this.areCellsInTheSameBox(candidates))
                    for (let cell of this.boxes[candidates[0].getBoxIndex()])
                        if (cell.getRowIndex() !== rowIndex)
                            cell.clean(digit);
            }
        }

        for (let col of this.cols) {
            let colIndex = col[0].getColIndex();

            // For this region, detect the digits whose cell is not decided yet.
            let decidedNumbers = new Set();
            for (let cell of col)
                if (cell.getCandidateAmount() === 1)
                    decidedNumbers.add(cell.show());

            let notDecidedNumbers = digits.diff(Array.from(decidedNumbers)).join("");

            // For each such digit, check which cells in the column have it as candidate...
            for (let digit of notDecidedNumbers) {
                let candidates = [];
                for (let cell of col)
                    if (cell.isCandidate(parseInt(digit)))
                        candidates.push(cell);

                // In the column region, if there are cell(s) which have this digit as a
                // candidate and all such cells are in the same box, then the other cells
                // (cells in a different column) in this box cannot have this digit as a candidate.
                if (candidates.length !== 0 && this.areCellsInTheSameBox(candidates))
                    for (let cell of this.boxes[candidates[0].getBoxIndex()])
                        if (cell.getColIndex() !== colIndex)
                            cell.clean(digit);
            }
        }
    }

    function areCellsInTheSameRow(cells)
    {
        for (let i = 0; i < cells.length - 1 ; i++)
            if (cells[i].getRowIndex() !== cells[i + 1].getRowIndex())
                return false;
        return true;
    }

    function areCellsInTheSameColumn(cells)
    {
        for (let i = 0; i < cells.length - 1 ; i++)
            if (cells[i].getColIndex() !== cells[i + 1].getColIndex())
                return false;
        return true;
    }

    function areCellsInTheSameBox(cells)
    {
        for (let i = 0; i < cells.length - 1; i++)
            if (cells[i].getBoxIndex() !== cells[i + 1].getBoxIndex())
                return false;
        return true;
    }

    function isFinished()
    {
        for (let cell of this.data)
            if (cell.getCandidateAmount() !== 1)
                return false;
        return true;
    }

    function isContradicting()
    {
        for (let cell of this.data)
            if (cell.getCandidateAmount() === 0)
                return true;
        return false;
    }

    function isThereMoreThanOneCandidate()
    {
        for (let cell of this.data)
            if (cell.getCandidateAmount() > 1)
                return true;
        return false;
    }

    function isChanged()
    {
        for (let cell of this.data)
            if (cell.isChanged())
                return true;
        return false;
    }

    /* 	If solved, returns 1
        If there is a contradiction, returns 0
        If there is a cell with multiple candidate, returns 2
        If there is a change, compared to the old state, returns 3
     */

    function solve() {

        this.initialize();
        this.nakedElimination();
        if (this.isContradicting())
            return 0;
        if (this.isChanged())
            return 3;

        for (let degree = 2; degree <= 4; degree++) {
            this.initialize();
            this.hiddenElimination(degree);
            if (this.isContradicting())
                return 0;
            if (this.isChanged())
                return 3;
        }

        this.initialize();
        this.pointingPair();
        if (this.isContradicting())
            return 0;
        if (this.isChanged())
            return 3;

        this.initialize();
        this.boxLineReduction();
        if (this.isContradicting())
            return 0;
        if (this.isChanged())
            return 3;

        if (this.isContradicting() || this.isThereFrequentCandidateListInAnyRegion())
            return 0;
        else if (this.isFinished())
            return 1;
        else if (this.isThereMoreThanOneCandidate())
            return 2;
    }

    /*
        Checks the summary of region (see summarizeRegion function)
        Returns true if there is a candidate list whose frequency is
        bigger than the length of the list.
        (We need to make this check because if there is such case, we can conclude that
        there is a contradiction in the puzzle, so either the initial puzzle is not solvable
        or after we tried a random value in a cell, it led to a contradiction so it must be reverted)
     */
    function isThereFrequentCandidateListInAnyRegion()
    {
        for (let region of this.allRegions)
            for (let element of this.summarizeRegion(region))
                if (element.getFrequency() > element.getCandidateList().length)
                    return true;
        return false;
    }

    function guessACell(is_first, copyData)
    {
        if (is_first)
        {
            copyData = [];
            dataSwap(copyData, this.data);
        }
        let counter = 2;
        while (counter <= 9)
        {
            for (let cell of copyData)
            {
                if (cell.getCandidateAmount() <= counter)
                {
                    while (cell.getCandidateAmount() > 1)
                    {
                        this.data = [];
                        dataSwap(this.data, copyData);
                        this.createRegionsData();

                        if (this.isThereFrequentCandidateListInAnyRegion() || this.isContradicting())
                            return false;

                        let values = cell.show();
                        let digit = values[0];
                        cell.removeAllButThem(digit);
                        this.data = [];
                        dataSwap(this.data, copyData);
                        this.createRegionsData();

                        let statusCode = 3;
                        while (statusCode === 3)
                            statusCode = this.solve();

                        if (statusCode === 0)
                        {
                            cell.assign(values);
                            cell.clean(digit);
                        }
                        if (statusCode === 1)
                            return true;
                        if (statusCode === 2)
                        {
                            let newDataCopy = [];
                            dataSwap(newDataCopy, this.data);
                            let success = this.guessACell(false, newDataCopy);
                            if (success)
                                return true;
                            else
                            {
                                cell.assign(values);
                                cell.clean(digit);
                            }
                        }
                    }
                }
            }
            counter++;
        }
        return false;
    }
}

function getSubsets(arr, output, subset_length, is_first) {

    if (typeof getSubsets.temp === 'undefined') {
        getSubsets.temp = "";
        getSubsets.last = 0;
    }

    if (is_first) {
        getSubsets.temp = "";
        getSubsets.last = subset_length;
    }

    for (let i = 0; (i < arr.length) && (i + subset_length <= arr.length); i++) {
        getSubsets.temp += arr[i];

        if (getSubsets.temp.length < getSubsets.last)
            getSubsets(arr.substr(i + 1, arr.length), output, subset_length - 1, false);

        if (getSubsets.temp.length === getSubsets.last)
            output.push(getSubsets.temp);

        getSubsets.temp = getSubsets.temp.substr(0, (getSubsets.temp.length - 1));
    }
}

function dataSwap(destinationData, sourceData) {

    for (let i = 0; i < 81; i++) {
        destinationData[i] = new Cell(i, "123456789");
        destinationData[i].changed = !!(sourceData[i].changed);
        destinationData[i].colIndex = i % 9;
        destinationData[i].rowIndex = Math.floor(i / 9);
        destinationData[i].boxIndex = (Math.floor(destinationData[i].rowIndex / 3) * 3) + Math.floor(destinationData[i].colIndex / 3);
        destinationData[i].numberOfCandidates = sourceData[i].numberOfCandidates;
        destinationData[i].candidates = [];
        for (let a = 0; a < 9; a++)
            destinationData[i].candidates[a] = !!(sourceData[i].candidates[a]);
    }
}