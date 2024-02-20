export interface Game {
    id: number;
    currentState: String[][];
    lastMoveRow: number;
    lastMoveColumn: number;
    type: String;
    
    finished: boolean;
    won: boolean;
    lost: boolean;
    drawn: boolean;

    username1: String;
    username2: String; 
    userStarts: boolean;
    currentSymbol: String;
}