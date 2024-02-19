import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { GameRequest } from './dto/game-request';
import { Observable } from 'rxjs';
import { GameResponse } from './dto/game-response';
import { Game } from './dto/game';
import { AcceptRequest } from './dto/accept-request';

@Injectable({
  providedIn: 'root'
})
export class GameManagementService {
  private apiUrl: String = "http://localhost:8000"

  constructor(protected http: HttpClient) { }

  public newGame(request: GameRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${this.apiUrl}/game`, request);
  }

  public getActiveGames(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.apiUrl}/game`);
  }

  public getGameRequests(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.apiUrl}/game/request`);
  }

  public getFinishedGames(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.apiUrl}/game/archive`);
  }
  
  public acceptRequest(gameId: number, request: AcceptRequest): Observable<Boolean> {
    return this.http.post<Boolean>(`${this.apiUrl}/game/${gameId}/request`, request);
  }
}