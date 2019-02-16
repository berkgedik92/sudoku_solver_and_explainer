let lineCursor;
let lines;
let regions;
let candidates;
let isWhite;
let greenCells;
let redCells;
let assigned;
let allLines;

function getCellInput(index) {
    return $("#cell_input" + index);
}

function getCandidateCell(index, candidate) {
    candidate = candidate.toString();
    return document.getElementById("k" + index + "c" + candidate);
}

/*Shadow Functions*/
function lightUpCells() {
    for (let i = 0; i < 9; i++)
        changeRegionShadow(i, false);
}

function lightDownCells() {
    for (let i = 0; i < 9; i++)
        changeRegionShadow(i, true);
}

//makes cells of the region with ID = regionID dark or light
function changeRegionShadow(regionID, isDark) {
    for (let i = 0; i < 9; i++)
        document.getElementById("k" + regions[regionID][i] + "s").style.display = (isDark) ? "block" : "none";
}

function resetColors() {
    for (let i = 0; i < greenCells.length; i++)
        getCandidateCell(greenCells[i][0], greenCells[i][1]).style.background = isWhite[greenCells[i][0]] ? "white" : "aqua";
        
    for (let i = 0; i < redCells.length; i++)
        eliminate(redCells[i][0], redCells[i][1]);
        
    aquas = new Array();
    whites = new Array();
    redCells = new Array();
}

function getClassNameForCandidateCell(candidateAmount) {
    if (candidateAmount >= 7) return "num";
    if (candidateAmount >= 5) return "num1";
    if (candidateAmount >= 3) return "num2";
    if (candidateAmount >= 2) return "num3";
    if (candidateAmount >= 1) return "num4";
}

function setCandidateCellStyle(cellID) {
    let candidateAmount = 0;

    // Count how many candidates this cell has
    for (let i = 0; i < 9; i++)
        if (candidates[cellID][i])
            candidateAmount++;

    let styleName = getClassNameForCandidateCell(candidateAmount);

    // Hide candidate cells if this cell does not have its value as candidate
    for (let i = 0; i < 9; i++) {
        if (candidates[cellID][i]) {
            getCandidateCell(cellID, i).className = styleName;
            getCandidateCell(cellID, i).style.display = "block";
        }
        else
            getCandidateCell(cellID, i).style.display = "none";
    }
}

function eliminate(cellID, value) {
    candidates[cellID][value] = false;
    getCandidateCell(cellID, value).style.background = isWhite[cellID] ? "white" : "aqua";
    setCandidateCellStyle(cellID);
}

function assignValue(cellID, value) {

    value = value.toString();

    let counter = 0;
    for (let i = 0; i < 9; i++)
        candidates[cellID][i] = false;
        
    for (let i = 0; i < value.length; i++)
        candidates[cellID][value[i] - 1] = true;
        
    setCandidateCellStyle(cellID);
}

function makeGreen(cellID, candidates) {
    candidates = ToString(candidates);
    for (let i = 0; i < candidates.length; i++) {
        greenCells.push([cellID, candidates[i] - 1]);
        getCandidateCell(cellID, candidates[i] - 1).style.background = "green";
    }
}

function makeRed(cellID, candidates) {
    candidates = ToString(candidates);
    for (let i = 0; i < candidates.length; i++) {
        redCells.push([cellID,candidates[i] - 1]);
        getCandidateCell(cellID, candidates[i] - 1).style.background = "red";
    }
}

function read() {

    lines = allLines.actions;

    regions     = new Array();
    candidates  = new Array();
    isWhite     = new Array();
    greenCells  = new Array();
    redCells    = new Array();
    assigned    = null;

    for (let i = 0; i < 81; i += 9)
        regions.push([i, i + 1, i + 2, i + 3, i + 4, i + 5, i + 6, i + 7, i + 8]);

    for (let i = 0; i < 9; i++)
        regions.push([i, i + 9, i + 18, i + 27, i + 36, i + 45, i + 54, i + 63, i + 72]);

    for (let i = 0; i < 9; i++) {
        let index = ((Math.floor(i / 3) * 3) * 9) + ((i % 3) * 3);
        regions.push([index, index + 1,index + 2, index + 9, index + 10, index + 11, index + 18, index + 19, index + 20]);
    }

    for (let i = 0; i < 81; i++) {
        candidates.push([true, true, true, true, true, true, true, true, true]);
        isWhite.push(false);
    }

    // Make all cells bright
    lightUpCells();

    // Read initial values of cells and display them on the screen
    for (let cell = 0; cell < 81; cell++) 
        if (allLines.initialPuzzle[cell] != 'x') {
            assignValue(cell, allLines.initialPuzzle[cell]);
            let numID = "k" + cell + "c" + (allLines.initialPuzzle[cell] - 1);
            document.getElementById(numID).style.background = "white"; 
            isWhite[cell] = true;
        }

    lineCursor = 0;
    document.getElementById("explain").innerHTML = "Ready to show the solution with explanations!";
}

function solve() {

    // Set the UI to explanation mode
    let container = $("#container").removeClass("initial").addClass("solving");

    $("#solve_button").css("display", "none");
    $("#clear_button").css("display", "none");
    $("#continue_button").css("display", "inline-block");

    let puzzle_string = "";
    for (let index = 0; index < 81; index++)
        if (getCellInput(index).val() !== "")
            puzzle_string += getCellInput(index).val();
        else
            puzzle_string += "x";

    $.ajax({
        url: "http://localhost:8080/solver/solve",
        type:'POST',
        contentType: "application/json; charset=UTF-8",
        data: puzzle_string,
        //data: "xxxxxx15xx4526xx78x2xxx7xx3x1x3xx6xxx9xx1xx8xxx2xx9x4x6xx4xxx1x17xx5823xx83xxxxxx",
        success:function (response) {
            allLines = response;
            read();
        },
        error:function (response) {
            console.log("Error")
            console.log(response);
       }
    });
}

function write(puzzle) {
    let values = puzzle.showValues();
    for (let cellIndex = 0; cellIndex < 81; cellIndex++)
        getCellInput(cellIndex).val(values[cellIndex]);
}

// Removes all inputs in cells
function reset() {
    for (let a = 0; a < 81 ; a++)
        getCellInput(a).val("");
}

for (let id = 0; id < 81; id++) {
	let row = Math.floor(id / 9);
	let col = id % 9;
	let box = (Math.floor(row / 3) * 3) + Math.floor(col / 3);
	let cl = (box % 2 === 0) ? "dark_cell" : "light_cell";

    //test
    let c = $("<div/>", {class: cl, id: "k" + id}).appendTo("#container");

    let shadow = $("<div/>", {class: "shadow", id: "k" + id + "s"});
    shadow.css("display", "none");
    c.append(shadow);
    
    let d = $("<input/>", {class: "cell_input", id: "cell_input" + id, maxlength: "1", type: "text"});
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

    $("#cell_input6").val("1");
    $("#cell_input7").val("5");
    $("#cell_input10").val("4");
    $("#cell_input11").val("5");
    $("#cell_input12").val("2");
    $("#cell_input13").val("6");
    $("#cell_input16").val("7");
    $("#cell_input17").val("8");
    $("#cell_input19").val("2");
    $("#cell_input23").val("7");
    $("#cell_input26").val("3");
    $("#cell_input28").val("1");
    $("#cell_input30").val("3");
    $("#cell_input33").val("6");
    $("#cell_input37").val("9");
    $("#cell_input40").val("1");
    $("#cell_input43").val("8");
    $("#cell_input47").val("2");
    $("#cell_input50").val("9");
    $("#cell_input52").val("4");
    $("#cell_input54").val("6");
    $("#cell_input57").val("4");
    $("#cell_input61").val("1");
    $("#cell_input63").val("1");
    $("#cell_input64").val("7");
    $("#cell_input67").val("5");
    $("#cell_input68").val("8");
    $("#cell_input69").val("2");
    $("#cell_input70").val("3");
    $("#cell_input73").val("8");
    $("#cell_input74").val("3");
}


function Next() {

    resetColors();

    document.getElementById("explain").innerHTML = (lineCursor) + "<br><br>";
    if (lineCursor == lines.length) {
        document.getElementById("explain").innerHTML += "End of solution";
        lightUpCells();
        return;
    }

    let action = lines[lineCursor++];

    lightDownCells();
    if (assigned != null) {
        assignValue (assigned[0],assigned[1]);
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
    changeRegionShadow(action.sourceID,false);
}

function HiddenElimination(action) {

    let tempRed = JSON.parse(JSON.stringify(action.redCells));
    action.redCells = "";

    for (let i = 0; i < 9; i++)
        if (tempRed[i] > 0)
            action.redCells += i + ":" + tempRed[i] + "-";
    
    document.getElementById("explain").innerHTML += IDtoName(action.sourceID) + " value(s) " + CandidatesParser(action.relatedCandidates) + " can be taken only by ";
    changeRegionShadow(action.sourceID,false);
    action.redCells = action.redCells.substr(0,action.redCells.length-1).split("-");
    let cells = (action.redCells.length>1) ? "the cells" : "the cell";
    document.getElementById("explain").innerHTML += " " + cells + " ";

    for (let i = 0; i < action.redCells.length; i++) {
        action.redCells[i] = action.redCells[i].split(":");
        let cellID = regions[action.sourceID][action.redCells[i][0]];
        document.getElementById("explain").innerHTML += "" + (parseInt(action.redCells[i][0]) + 1) + ". ";
        makeGreen(cellID,action.relatedCandidates);
        makeRed(cellID,action.redCells[i][1]);
    }
    document.getElementById("explain").innerHTML += ". For this reason, " + cells + " must only have " + CandidatesParser(action.relatedCandidates) + ".";
}

function BoxReduction(action) {

    let tempGreen = JSON.parse(JSON.stringify(action.greenCells));
    let tempRed = JSON.parse(JSON.stringify(action.redCells));

    action.greenCells = "";
    action.redCells = "";

    for (let i = 0; i < 9; i++)
        if (tempGreen[i] > 0)
            action.greenCells += i + "";

    for (let i = 0; i < 9; i++)
        if (tempRed[i] > 0)
            action.redCells += i + ":" + tempRed[i] + "-";

    action.greenCells = action.greenCells.trim();
    action.redCells = action.redCells.trim();

    document.getElementById("explain").innerHTML += IDtoName(action.sourceID) + " all cell(s) that can have " + CandidatesParser(action.relatedCandidates) + " reside(s) at " + IDtoName(action.targetID) + ". For this reason, the cell(s) which are " + IDtoName(action.targetID) + " and not " + IDtoName(action.sourceID) + " must not have " + CandidatesParser(action.relatedCandidates) + ".";
    changeRegionShadow(action.sourceID,false);
    changeRegionShadow(action.targetID, false);
    action.redCells = action.redCells.substr(0,action.redCells.length-1).split("-");
    for (let i = 0; i < action.redCells.length; i++) {
        action.redCells[i] = action.redCells[i].split(":");
        let cellID = regions[action.targetID][action.redCells[i][0]];
        makeRed(cellID, action.redCells[i][1]);
    }
    for (let i = 0; i < action.greenCells.length; i++) {
        let cellID = regions[action.targetID][action.greenCells[i]];
        console.log(cellID);
        makeGreen(cellID, action.relatedCandidates);
    }
}

function Rollback(action) {
    document.getElementById("explain").innerHTML += "We restored to the latest state without contradiction (before our guess). Thanks to the fact that this guess led us to a contradiction, the cell" + IDtoName(action.sourceID) + " cannot have " + action.relatedCandidates + " as value.";
    lightUpCells();
    for (let i = 0; i < 81; i++)
        assignValue(i,action.values[i]);
    makeRed(action.sourceID,action.relatedCandidates);
}

function Trying(action) {
    document.getElementById("explain").innerHTML += "We cannot make any further elimination by using logical inference. We have to guess. At cell " + IDtoName(action.sourceID) + " we will try the value " + action.relatedCandidates + " . If we will get a contradiction after that, we will restore to this state.";
    lightUpCells();
    makeGreen(IDtoName(action.sourceID),action.relatedCandidates);
    assigned = [IDtoName(action.sourceID),action.relatedCandidates];
}

function NakedElimination(action) {

    let tempGreen = JSON.parse(JSON.stringify(action.greenCells));
    let tempRed = JSON.parse(JSON.stringify(action.redCells));

    action.greenCells = "";
    action.redCells = "";

    for (let i = 0; i < 9; i++)
        if (tempGreen[i] > 0)
            action.greenCells += i + "";

    for (let i = 0; i < 9; i++)
        if (tempRed[i] > 0)
            action.redCells += i + ":" + tempRed[i] + "-";

    action.greenCells = action.greenCells.trim();
    action.redCells = action.redCells.trim();

    document.getElementById("explain").innerHTML += IDtoName(action.sourceID);
    changeRegionShadow(action.sourceID,false);
    let cells = (action.greenCells.length > 1) ? "cells " : "cell ";
    document.getElementById("explain").innerHTML += cells;
    for (let i = 0; i < action.greenCells.length; i++) {
        let cellID = regions[action.sourceID][action.greenCells[i]];
        document.getElementById("explain").innerHTML += "" + (parseInt(action.greenCells[i][0]) + 1) + " "
        makeGreen(cellID, action.relatedCandidates);
    }
    document.getElementById("explain").innerHTML += " can only have " + CandidatesParser(action.relatedCandidates) + ". For this reason, the other cell(s) in this region must not have " + CandidatesParser(action.relatedCandidates) + ".";
    
    action.redCells = action.redCells.substr(0, action.redCells.length - 1).split("-");
    for (let i = 0; i < action.redCells.length; i++)
        action.redCells[i] = action.redCells[i].split(":");
    for (let i = 0; i < action.redCells.length; i++) {
        let cellID = regions[action.sourceID][action.redCells[i][0]];
        makeRed(cellID,action.redCells[i][1]);
    }
}

function IDtoName (regionID) {
    let result = "At ";
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

    let result = "";
    let mask = 1;
    for (let i = 0; i < 9; i++, mask = mask << 1)
        if ((mask & relatedCandidates) >= 1)
            result += (i + 1) + "";
    relatedCandidates = result;
    result = "";
    for (let i = 0; i < relatedCandidates.length - 1; i++)
        result += relatedCandidates[i] + " ";
    result += relatedCandidates[relatedCandidates.length - 1];
    result = ((relatedCandidates.length > 1) ? " the values " : " the value ") + result;
    return result;
}

function ToString(data) {
    let result = "";
    let mask = 1;
    for (let i = 0; i < 9; i++, mask = mask << 1)
        if ((mask & data) >= 1)
            result += (i + 1) + "";
    return result;
}

enterHardPuzzle();
