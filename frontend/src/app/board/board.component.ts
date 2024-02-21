import { Component, Input, OnInit } from '@angular/core';
import { WebsocketService } from './websocket.service';
import { BoardMessage } from './dto/board-message';

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
  ]


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
    this.websocket.board$.subscribe((board: BoardMessage) => {
      this.board = board.state;
      console.log(board);
    });
  }

  move(row: number, column: number) {
    if (this._gameId == undefined) {
      return;
    }
    this.websocket.makeMove(this._gameId, { x: row, y: column });
    console.log(row, ", ", column)
  }

}
