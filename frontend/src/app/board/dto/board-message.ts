export interface BoardMessage {
    username1: String;
    username2: String;
    ai: boolean;

    state: String[][];
    lastMoveX: number;
    lastMoveY: number;
    currentSymbol: String;
    currentPlayer: String;

    error?: String;
}