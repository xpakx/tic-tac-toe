export interface Game {
    id: number;
    currentState: String; //todo
    lastMove: String; //todo
    type: String;
    
    finished: boolean;
    won: boolean;
    lost: boolean;
    drawn: boolean;

    username1: String; //todo
    username2: String; 
    userStarts: boolean;
    currentSymbol: String;
}