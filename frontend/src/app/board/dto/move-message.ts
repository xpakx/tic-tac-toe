export interface MoveMessage {
    player: String;
    x: number;
    y: number;
    currentSymbol: String;
    legal: boolean;
    applied: boolean;
    message?: String;
    finished: boolean ;
    drawn: boolean ;
    won: boolean ;
    winner?: String;
}