import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-board',
  templateUrl: './board.component.html',
  styleUrls: ['./board.component.css']
})
export class BoardComponent implements OnInit {
  _gameId?: number;
  @Input() set gameId(value: number | undefined) {
    this._gameId = value;
    console.log(value);
  }

  constructor() { }

  ngOnInit(): void {
  }

}
