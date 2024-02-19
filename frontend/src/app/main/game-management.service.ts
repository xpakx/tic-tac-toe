import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { GameRequest } from './dto/game-request';
import { Observable } from 'rxjs';
import { GameResponse } from './dto/game-response';
import { Game } from './dto/game';
import { AcceptRequest } from './dto/accept-request';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class GameManagementService {
  private apiUrl: String = environment.apiUrl + "/game";

  constructor(protected http: HttpClient) { }

  public newGame(request: GameRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${this.apiUrl}`, request);
  }

  public getActiveGames(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.apiUrl}`);
  }

  public getGameRequests(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.apiUrl}/request`);
  }

  public getFinishedGames(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.apiUrl}/archive`);
  }
  
  public acceptRequest(gameId: number, request: AcceptRequest): Observable<Boolean> {
    return this.http.post<Boolean>(`${this.apiUrl}/${gameId}/request`, request);
  }
}