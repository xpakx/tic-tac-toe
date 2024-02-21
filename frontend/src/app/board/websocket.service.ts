import { Injectable } from '@angular/core';
import { IMessage, RxStomp } from '@stomp/rx-stomp';
import { Observable, Subject, Subscription } from 'rxjs';
import { environment } from 'src/environments/environment';
import { MoveRequest } from './dto/move-request';
import { BoardMessage } from './dto/board-message';
import { MoveMessage } from './dto/move-message';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private apiUrl: String;
  connected: boolean = false;
  rxStomp?: RxStomp;

  private boardSubject: Subject<BoardMessage> = new Subject<BoardMessage>();
  private boardQueue?: Subscription;
  private boardOOB?: Subscription;
  board$: Observable<BoardMessage> = this.boardSubject.asObservable();

  private moveSubject: Subject<MoveMessage> = new Subject<MoveMessage>();
  private moveQueue?: Subscription;
  move$: Observable<MoveMessage> = this.moveSubject.asObservable();


  constructor() { 
    this.apiUrl = environment.apiUrl.replace(/^http/, 'ws');
  }

  connect() {
    if (this.connected) {
      return;
    }
    let url = this.apiUrl + "/play/websocket";
    let tokenFromStorage = localStorage.getItem("token");
    let token = tokenFromStorage == null ? "" : tokenFromStorage;
    
    this.rxStomp = new RxStomp();
    this.rxStomp.configure({
      brokerURL: url,
      connectHeaders: {
        Token: token,
      },

      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      reconnectDelay: 500,

      debug: (msg: string): void => {
        console.log(new Date(), msg);
      },
    });

    console.log("activating");
    this.rxStomp.activate();
    this.connected = true; // TODO
  }

  makeMove(gameId: number, move: MoveRequest) {
    if(this.rxStomp == undefined) {
      return;
    }
    this.rxStomp.publish({ destination: `/app/move/${gameId}`, body: JSON.stringify(move) });
  }

  subscribeGame(gameId: number) {
    this.subscribeMoves(gameId);
    this.subscribeBoard(gameId);
    this.subscribeChat(gameId);
  }

  subscribeMoves(gameId: number) {
    if(this.rxStomp == undefined) {
      return;
    }
    this.moveQueue = this.rxStomp
      .watch(`/topic/game/${gameId}`)
      .subscribe((message: IMessage) => {
        let move: MoveMessage = JSON.parse(message.body)
        this.moveSubject.next(move);
      });
  }

  subscribeBoard(gameId: number) {
    if(this.rxStomp == undefined) {
      return;
    }
    this.boardOOB = this.rxStomp
      .watch(`/app/board/${gameId}`)
      .subscribe((message: IMessage) => {
        let board: BoardMessage = JSON.parse(message.body)
        this.boardSubject.next(board);
      });
    this.boardQueue = this.rxStomp
      .watch(`/topic/board/${gameId}`)
      .subscribe((message: IMessage) => {
        let board: BoardMessage = JSON.parse(message.body)
        this.boardSubject.next(board);
      });
  }


  subscribeChat(gameId: number) {
    if(this.rxStomp == undefined) {
      return;
    }
    this.rxStomp
      .watch(`/topic/chat/${gameId}`)
      .subscribe((message: IMessage) => {
        console.log(message.body);
      });
  }
}
