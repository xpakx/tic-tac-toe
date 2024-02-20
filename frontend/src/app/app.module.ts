import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';
import { ModalRegisterComponent } from './auth/modal-register/modal-register.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from  '@angular/common/http';
import { ModalLoginComponent } from './auth/modal-login/modal-login.component';
import { BoardComponent } from './board/board.component';
import { MenuComponent } from './menu/menu.component';
import { GameListComponent } from './game-list/game-list.component';
import { NewGameModalComponent } from './new-game-modal/new-game-modal.component';

@NgModule({
  declarations: [
    AppComponent,
    ModalRegisterComponent,
    ModalLoginComponent,
    BoardComponent,
    MenuComponent,
    GameListComponent,
    NewGameModalComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
