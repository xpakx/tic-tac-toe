import { Component, Input, OnInit } from '@angular/core';
import { WebsocketService } from './websocket.service';
import { BoardMessage } from './dto/board-message';
import { MoveMessage } from './dto/move-message';
import { Subscription } from 'rxjs';
import { Game } from '../main/dto/game';

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
  game?: BoardMessage;
  error: String[][] = [
    ["", "", ""],
    ["", "", ""],
    ["", "", ""]
  ];
  private moveSub?: Subscription;
  private boardSub?: Subscription;

  finished: boolean = false;
  msg: String = "";

  @Input() set gameId(value: number | undefined) {
    this._gameId = value;
    console.log(value);
    this.finished = false;
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
      this.game = board;
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

    if (move.finished) {
      this.finished = true;
      if (move.drawn) {
        console.log("Draw!");
        this.msg = "Draw!";
      } else if (move.finished) {
        console.log(`${move.winner} won!`);
        let currentUser = localStorage.getItem("username");
        if(currentUser == move.winner) {
          this.msg = "You won!";
        } else if (currentUser == this.game?.username1 || currentUser == this.game?.username2) {
          this.msg = "You lost!";
        } else {
          this.msg = `${move.winner} won!`;
        }
      } 
    }

  }

}
