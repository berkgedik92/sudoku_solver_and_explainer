var lineCursor;    //keeps which line will be readed
var lines;        //keeps lines
var regions;    //keeps which region has which cells
var candidates; //keeps which cell has how many candidates
var isWhite;
var greens;
var reds;
var assigned;
var alllines;

function getCellDOM(index) {
    return $("#cell" + index);
}

/*SHADOW FUNCTIONS*/
function AllLight() {
    for (var i=0;i<9;i++)
        ChangeShadow(i,false);
}

function AllDark() {
    for (var i=0;i<9;i++)
        ChangeShadow(i,true);
}

//makes cells of "regionID" dark or light
function ChangeShadow (regionID, isDark) {
    for (var i=0;i<9;i++)
        document.getElementById("k"+regions[regionID][i]+"s").style.display = ((isDark) ? "block" : "none");
}
/*SHADOW FUNCTIONS FINISHED*/

/*CELL CANDIDATES FUNCTION*/
function ResetColors() {
    for (var i=0;i<greens.length;i++) 
        document.getElementById("k"+greens[i][0]+"c"+greens[i][1]).style.background = (isWhite[greens[i][0]] ? "white" : "aqua");
    for (var i=0;i<reds.length;i++)
        Eliminate(reds[i][0],reds[i][1]);
    aquas = new Array();
    whites = new Array();
    reds = new Array();
}

function GiveCandidateClass (counter) {
    if (counter>=7) return "num";
    if (counter>=5) return "num1";
    if (counter>=3) return "num2";
    if (counter>=2) return "num3";
    if (counter>=1) return "num4";
}

function AlignCell (cellID) {
    var counter = 0;
    for (var i=0;i<9;i++)
        if (candidates[cellID][i])
            counter++;
    var styleName = GiveCandidateClass(counter);
    for (var i=0;i<9;i++) {
        var candID = "k" + cellID + "c" + i;
        if (candidates[cellID][i]) {
            document.getElementById(candID).className = styleName;
            document.getElementById(candID).style.display = "block";
        }
        else
            document.getElementById(candID).style.display = "none";
    }
}

function Eliminate (cellID, value) {
    candidates[cellID][value] = false;
    document.getElementById("k"+cellID+"c"+value).style.background = (isWhite[cellID] ? "white" : "aqua");
    AlignCell(cellID);
}

function AssignValue (cellID, value) {
    var counter = 0;
    for (var i=0;i<9;i++)
        candidates[cellID][i] = false;
    for (var i=0;i<value.length;i++)
        candidates[cellID][value[i]-1] = true;
    AlignCell(cellID);
}

function MakeGreen (cellID, candidates) {
    candidates = ToString(candidates);
    for (var i=0;i<candidates.length;i++) {
        var id = "k" + cellID + "c" + (candidates[i]-1);
        greens.push([cellID,candidates[i]-1]);
        document.getElementById(id).style.background = "green";
    }
}

function MakeRed (cellID, candidates) {
    candidates = ToString(candidates);
    for (var i=0;i<candidates.length;i++) {
        var id = "k" + cellID + "c" + (candidates[i]-1);
        reds.push([cellID,candidates[i]-1]);
        document.getElementById(id).style.background = "red";
    }
}

function Read() {

    lines = alllines.actions;

    //Initialise arrays
    regions = new Array();
    candidates = new Array();
    isWhite = new Array();
    greens = new Array();
    reds = new Array();
    assigned = null;
    for (var i=0; i<81; i+=9)
        regions.push([i,i+1,i+2,i+3,i+4,i+5,i+6,i+7,i+8]);

    for (var i=0; i<9; i++) 
        regions.push([i,i+9,i+18,i+27,i+36,i+45,i+54,i+63,i+72]);

    for (var i=0; i<9; i++) {
        var ind = ((Math.floor(i/3)*3)*9) + ((i%3)*3); 
        regions.push([ind,ind+1,ind+2,ind+9,ind+10,ind+11,ind+18,ind+19,ind+20]);
    }

    for (var i=0;i<81;i++) {
        candidates.push([true,true,true,true,true,true,true,true,true]);
        isWhite.push(false);
    }

    //Make all cells light
    AllLight();

    //Read initial values of cells and display them on screen
    for (var cell=0;cell<81;cell++) 
        if (alllines.intro[cell] != 'x') {
            AssignValue(cell,"" + alllines.intro[cell]);
            var numID = "k"+cell+"c"+(alllines.intro[cell]-1);
            document.getElementById(numID).style.background = "white"; 
            isWhite[cell] = true;
        }

    lineCursor = 0;
    document.getElementById("explain").innerHTML = "Ready!";
}

function solve() {

    //Set to solver mode
    let container = $("#container").removeClass("initial").addClass("solving");

    $("#solve_button").css("display", "none");
    $("#clear_button").css("display", "none");
    $("#continue_button").css("display", "inline-block");

    $.ajax({
        url: "http://localhost:8080/solver/solve",
        type:'POST',
        contentType: "application/json; charset=UTF-8",
        data: "xxxxxx15xx4526xx78x2xxx7xx3x1x3xx6xxx9xx1xx8xxx2xx9x4x6xx4xxx1x17xx5823xx83xxxxxx",
        success:function (response) {
            alllines = response;
            console.log(response);
            Read();
        },
        error:function (response) {
            console.log("Error")
            console.log(response);
       }
    });

    let puzzle_string = "";
    for (let index = 0; index < 81; index++)
        if (getCellDOM(index).val() !== "")
            puzzle_string += getCellDOM(index).val();
        else
            puzzle_string += " ";

    let puzzle = new Sudoku(puzzle_string);
    let status_code = 3;
    while (status_code === 3)
        status_code = puzzle.solve();

    if (status_code === 0)
        alert("This sudoku has no solution!");
    else if (status_code === 1)
        write(puzzle);
    else if (status_code === 2) {
        let d = [];
        if (puzzle.guessACell(true, d)) {
            status_code = 1;
            write(puzzle);
        }
        else
            alert("Sudoku cannot be solved!");
    }

    return {
        "status": status_code,
        "solution": puzzle.showValues().join("")
    };
}

function write(puzzle) {
    let values = puzzle.showValues();
    for (let cellIndex = 0; cellIndex < 81; cellIndex++)
        getCellDOM(cellIndex).val(values[cellIndex]);
}

// Removes all inputs in cells
function reset() {
    for (let a = 0; a < 81 ; a++)
        getCellDOM(a).val("");
}

for (let id = 0; id < 81; id++) {
	let row = Math.floor(id / 9);
	let col = id % 9;
	let box = (Math.floor(row / 3) * 3) + Math.floor(col / 3);
	let cl = (box % 2 === 0) ? "dark_cell" : "light_cell";

    //test
    var c = $("<div/>", {class: cl, id: "k" + id}).appendTo("#container");

    let shadow = $("<div/>", {class: "shadow", id: "k" + id + "s"});
    shadow.css("display", "none");
    c.append(shadow);
    
    var d = $("<input/>", {class: "cell_input", id: "cell" + id, maxlength: "1", type: "text"});
    c.append(d);
}

for (let i = 0; i < 81; i++) {
    let cell = $("#k" + i);
    for (let j = 0; j < 9; j++) {
        let numcell = $("<div/>", {class: "num", id: "k" + i + "c" + j});
        numcell.html(j + 1);
        cell.append(numcell);
    }
}

/*
    Triggering this function will automatically
    enter a very hard sudoku problem into UI, for the problem see:
    https://www.conceptispuzzles.com/index.aspx?uri=info/article/424
*/
function enterHardPuzzle() {

   /*
    xxxxxx15x
    x4526xx78
    x2xxx7xx3
    x1x3xx6xx
    x9xx1xx8x
    xx2xx9x4x
    6xx4xxx1x
    17xx5823x
    x83xxxxxx
    */

    $("#cell6").val("1");
    $("#cell7").val("5");
    $("#cell10").val("4");
    $("#cell11").val("5");
    $("#cell12").val("2");
    $("#cell13").val("6");
    $("#cell16").val("7");
    $("#cell17").val("8");
    $("#cell19").val("2");
    $("#cell23").val("7");
    $("#cell26").val("3");
    $("#cell28").val("1");
    $("#cell30").val("3");
    $("#cell33").val("6");
    $("#cell37").val("9");
    $("#cell40").val("1");
    $("#cell43").val("8");
    $("#cell47").val("2");
    $("#cell50").val("9");
    $("#cell52").val("4");
    $("#cell54").val("6");
    $("#cell57").val("4");
    $("#cell61").val("1");
    $("#cell63").val("1");
    $("#cell64").val("7");
    $("#cell67").val("5");
    $("#cell68").val("8");
    $("#cell69").val("2");
    $("#cell70").val("3");
    $("#cell73").val("8");
    $("#cell74").val("3");
}


function Next() {

    ResetColors();

    document.getElementById("explain").innerHTML = (lineCursor) + "<br><br>";
    if (lineCursor == lines.length) {
        document.getElementById("explain").innerHTML += "End of solution";
        AllLight();
        return;
    }

    var action = lines[lineCursor++];

    AllDark();
    if (assigned != null) {
        AssignValue (assigned[0],assigned[1]);
        assigned = null;
    }

    if (action.event === "NAKED_ELIMINATION")
        NakedElimination(action);
    else if (action.event === "HIDDEN_ELIMINATION")
        HiddenElimination(action);
    else if (action.event === "BOX_REDUCTION" || action.event === "POINTING_PAIR")
        BoxReduction(action);
    else if (action.event === "TRYING")
        Trying(action);
    else if (action.event === "ROLLBACK")
        Rollback(action);
    else if (action.event === "CONTRADICTION")
        Contradiction(action);
}

function Contradiction(action) {
    document.getElementById("explain").innerHTML += "Contradiction : " + IDtoName(action.sourceID) + " there is no cell which can have " + CandidatesParser(action.relatedCandidates) + "as candidate, so the latest guess led us to a contradiction. We will restore the latest state free of contradictions.";
    ChangeShadow(action.sourceID,false);
}

function HiddenElimination(action) {

    var tempRed = JSON.parse(JSON.stringify(action.redCells));
    action.redCells = "";

    for (var i = 0; i < 9; i++)
        if (tempRed[i] > 0)
            action.redCells += i + ":" + tempRed[i] + "-";
    
    document.getElementById("explain").innerHTML += IDtoName(action.sourceID) + " value(s) " + CandidatesParser(action.relatedCandidates) + " can be taken only by ";
    ChangeShadow(action.sourceID,false);
    action.redCells = action.redCells.substr(0,action.redCells.length-1).split("-");
    var cells = (action.redCells.length>1) ? "the cells" : "the cell";
    document.getElementById("explain").innerHTML += " " + cells + " ";
    for (var i=0;i<action.redCells.length;i++) {
        action.redCells[i] = action.redCells[i].split(":");
        var cellID = regions[action.sourceID][action.redCells[i][0]];
        document.getElementById("explain").innerHTML += "" + (parseInt(action.redCells[i][0]) + 1) + ". ";
        MakeGreen(cellID,action.relatedCandidates);
        MakeRed(cellID,action.redCells[i][1]);
    }
    document.getElementById("explain").innerHTML += ". For this reason, " + cells + " must only have " + CandidatesParser(action.relatedCandidates) + ".";
}

function BoxReduction(action) {

    var tempGreen = JSON.parse(JSON.stringify(action.greenCells));
    var tempRed = JSON.parse(JSON.stringify(action.redCells));

    action.greenCells = "";
    action.redCells = "";

    for (var i = 0; i < 9; i++)
        if (tempGreen[i] > 0)
            action.greenCells += i + "";

    for (var i = 0; i < 9; i++)
        if (tempRed[i] > 0)
            action.redCells += i + ":" + tempRed[i] + "-";

    action.greenCells = action.greenCells.trim();
    action.redCells = action.redCells.trim();

    document.getElementById("explain").innerHTML += IDtoName(action.sourceID) + " all cell(s) that can have " + CandidatesParser(action.relatedCandidates) + " reside(s) at " + IDtoName(action.targetID) + ". For this reason, the cell(s) which are " + IDtoName(action.targetID) + " and not " + IDtoName(action.sourceID) + " must not have " + CandidatesParser(action.relatedCandidates) + ".";
    ChangeShadow(action.sourceID,false);
    ChangeShadow(action.targetID, false);
    action.redCells = action.redCells.substr(0,action.redCells.length-1).split("-");
    for (var i=0;i<action.redCells.length;i++) {
        action.redCells[i] = action.redCells[i].split(":");
        var cellID = regions[action.targetID][action.redCells[i][0]];
        MakeRed(cellID, action.redCells[i][1]);
    }
    for (var i=0;i<action.greenCells.length;i++) {
        var cellID = regions[action.targetID][action.greenCells[i]];
        console.log(cellID);
        if (!cellID) {
            var a = 1;
        }
        MakeGreen(cellID, action.relatedCandidates);
    }
}

function Rollback(action) {
    document.getElementById("explain").innerHTML += "We restored to the latest state without contradiction (before our guess). Thanks to the fact that this guess led us to a contradiction, the cell" + IDtoName(action.sourceID) + " cannot have " + action.relatedCandidates + " as value.";
    AllLight();
    for (var i=0;i<81;i++)
        AssignValue(i,action.values[i]);
    MakeRed(action.sourceID,action.relatedCandidates);
}

function Trying(action) {
    document.getElementById("explain").innerHTML += "We cannot make any further elimination by using logical inference. We have to guess. At cell " + IDtoName(action.sourceID) + " we will try the value " + action.relatedCandidates + " . If we will get a contradiction after that, we will restore to this state.";
    AllLight();
    MakeGreen(IDtoName(action.sourceID),action.relatedCandidates);
    assigned = [IDtoName(action.sourceID),action.relatedCandidates];
}

function NakedElimination(action) {

    var tempGreen = JSON.parse(JSON.stringify(action.greenCells));
    var tempRed = JSON.parse(JSON.stringify(action.redCells));

    action.greenCells = "";
    action.redCells = "";

    for (var i = 0; i < 9; i++)
        if (tempGreen[i] > 0)
            action.greenCells += i + "";

    for (var i = 0; i < 9; i++)
        if (tempRed[i] > 0)
            action.redCells += i + ":" + tempRed[i] + "-";

    action.greenCells = action.greenCells.trim();
    action.redCells = action.redCells.trim();

    document.getElementById("explain").innerHTML += IDtoName(action.sourceID);
    ChangeShadow(action.sourceID,false);
    var cells = (action.greenCells.length > 1) ? "cells " : "cell ";
    document.getElementById("explain").innerHTML += cells;
    for (var i=0;i<action.greenCells.length;i++) {
        var cellID = regions[action.sourceID][action.greenCells[i]];
        document.getElementById("explain").innerHTML += "" + (parseInt(action.greenCells[i][0]) + 1) + " "
        MakeGreen(cellID, action.relatedCandidates);
    }
    document.getElementById("explain").innerHTML += " can only have " + CandidatesParser(action.relatedCandidates) + ". For this reason, the other cell(s) in this region must not have " + CandidatesParser(action.relatedCandidates) + ".";
    
    action.redCells = action.redCells.substr(0, action.redCells.length - 1).split("-");
    for (var i=0;i<action.redCells.length;i++)
        action.redCells[i] = action.redCells[i].split(":");
    for (var i=0;i<action.redCells.length;i++) {
        var cellID = regions[action.sourceID][action.redCells[i][0]];
        MakeRed(cellID,action.redCells[i][1]);
    }
}

function IDtoName (regionID) {
    var result = "At ";
    regionID = parseInt(regionID);
    if (regionID < 9) {
        result += "Row ";
        regionID += 1;
    }
    else if (regionID < 18) {
        result += "Column ";
        regionID -= 8;
    }
    else {
        result += "Box "; 
        regionID -= 17;
    }
    result += regionID + "";
    return result;
}

function CandidatesParser(relatedCandidates) {

    var result = "";
    var mask = 1;
    for (var i = 0; i < 9; i++, mask = mask << 1)
        if ((mask & relatedCandidates) >= 1)
            result += (i + 1) + "";
    relatedCandidates = result;
    result = "";
    for (var i = 0; i < relatedCandidates.length - 1; i++)
        result += relatedCandidates[i] + " ";
    result += relatedCandidates[relatedCandidates.length - 1];
    result = ((relatedCandidates.length > 1) ? " the values " : " the value ") + result;
    return result;
}

function ToString(data) {
    var result = "";
    var mask = 1;
    for (var i = 0; i < 9; i++, mask = mask << 1)
        if ((mask & data) >= 1)
            result += (i + 1) + "";
    return result;
}

enterHardPuzzle();