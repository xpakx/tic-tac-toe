import { Component, Input, OnInit } from '@angular/core';
import { WebsocketService } from './websocket.service';
import { BoardMessage } from './dto/board-message';
import { MoveMessage } from './dto/move-message';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-board',
  templateUrl: './board.component.html',
  styleUrls: ['./board.component.css']
})
export class BoardComponent implements OnInit {
  _gameId?: number;
  board: String[][] = [
    ["X", "", ""],
    ["", "O", ""],
    ["", "Empty", ""]
  ];
  error: String[][] = [
    ["", "", ""],
    ["", "", ""],
    ["", "", ""]
  ];
  private moveSub?: Subscription;
  private boardSub?: Subscription;


  @Input() set gameId(value: number | undefined) {
    this._gameId = value;
    console.log(value);
    if (this._gameId) {
      console.log("calling websocket");
      this.websocket.connect();
      this.websocket.subscribeGame(this._gameId);
    }
  }

  constructor(private websocket: WebsocketService) { }

  ngOnInit(): void {
    this.boardSub = this.websocket.board$.subscribe((board: BoardMessage) => {
      this.board = board.state;
      console.log(board);
    });

    this.moveSub = this.websocket.move$.subscribe((move: MoveMessage) => {
      this.makeMove(move);
      console.log(move);
    });
  }

  ngOnDestroy() {
    this.websocket.unsubscribe();
    this.websocket.disconnect();
    this.boardSub?.unsubscribe();
    this.moveSub?.unsubscribe();
  }

  move(row: number, column: number) {
    if (this._gameId == undefined) {
      return;
    }
    this.websocket.makeMove(this._gameId, { x: row, y: column });
    console.log(row, ", ", column)
  }

  makeMove(move: MoveMessage) {
    this.error = [["", "", ""], ["", "", ""], ["", "", ""]];
    if (!move.applied) {
      if (!move.legal) {
        this.error[move.x][move.y] = "illegal";
      }
      return;
    }
    this.board[move.x][move.y] = move.currentSymbol;

  }

}
