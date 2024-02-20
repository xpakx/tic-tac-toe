import { Component, Input, OnInit } from '@angular/core';
import { Game } from '../main/dto/game';

@Component({
  selector: 'app-game-list',
  templateUrl: './game-list.component.html',
  styleUrls: ['./game-list.component.css']
})
export class GameListComponent implements OnInit {
  @Input() games: Game[] = [];
  @Input() active: boolean = true;
  @Input() requests: boolean = false;


  constructor() { }

  ngOnInit(): void {
  }

}
