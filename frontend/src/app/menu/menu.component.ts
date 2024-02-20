import { Component, OnInit } from '@angular/core';
import { Game } from '../main/dto/game';
import { GameManagementService } from '../main/game-management.service';
import { HttpErrorResponse } from '@angular/common/http';
import { GameResponse } from '../main/dto/game-response';

@Component({
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit {
  games: Game[] = [];
  requestView: boolean = false;
  activeView: boolean = false;
  error: boolean = false;
  errorMsg: String = "";

  constructor(private gameService: GameManagementService) { }

  ngOnInit(): void {
  }

  getRequests() {
    this.gameService.getGameRequests()
      .subscribe({
        next: (games: Game[]) => this.onRequests(games),
        error: (err: HttpErrorResponse) => this.onError(err)
      });
  }

  getGames() {
    this.gameService.getActiveGames()
      .subscribe({
        next: (games: Game[]) => this.onGames(games),
        error: (err: HttpErrorResponse) => this.onError(err)
      });
  }

  getArchive() {
    this.gameService.getFinishedGames()
      .subscribe({
        next: (games: Game[]) => this.onArchive(games),
        error: (err: HttpErrorResponse) => this.onError(err)
      });

  }

  onRequests(games: Game[]) {
    this.games = games;
    this.activeView = false;
    this.requestView = true;
  }

  onArchive(games: Game[]) {
    this.games = games;
    this.activeView = false;
    this.requestView = false;
  }

  onGames(games: Game[]) {
    this.games = games;
    this.activeView = true;
    this.requestView = false;
  }

  onError(err: HttpErrorResponse) {
    console.log(err);
    this.error = true;
    this.errorMsg = err.message;
  }
  
  newGame() {

  }

  newAIGame() {
    this.gameService.newGame({type: "AI"})
      .subscribe({
        next: (game: GameResponse) => this.open(game.id),
        error: (err: HttpErrorResponse) => this.onError(err)
      });
  }

  open(gameId: number) {
    // todo
  }
}
