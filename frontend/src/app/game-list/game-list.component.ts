import { Component, Input, OnInit } from '@angular/core';
import { Game } from '../main/dto/game';
import { GameManagementService } from '../main/game-management.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-game-list',
  templateUrl: './game-list.component.html',
  styleUrls: ['./game-list.component.css']
})
export class GameListComponent implements OnInit {
  @Input() games: Game[] = [];
  @Input() active: boolean = true;
  @Input() requests: boolean = false;

  constructor(private gameService: GameManagementService) { }

  ngOnInit(): void {
  }

  accept(gameId: number) {
    this.gameService.acceptRequest(gameId, {accepted: true})
      .subscribe({
        next: (value: Boolean) => this.onAccept(gameId),
        error: (err: HttpErrorResponse) => this.onError(err)
      });
  }

  onAccept(gameId: number) {
    this.open(gameId);
  }

  reject(gameId: number) {
    this.gameService.acceptRequest(gameId, {accepted: false})
      .subscribe({
        next: (value: Boolean) => this.onReject(gameId),
        error: (err: HttpErrorResponse) => this.onError(err)
      });

  }

  onReject(gameId: number) {
    // todo
  }

  onError(err: HttpErrorResponse) {
    // todo
  }

  open(gameId: number) {

  }
}
