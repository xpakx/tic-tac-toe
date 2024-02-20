import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';

@Component({
  selector: 'app-new-game-modal',
  templateUrl: './new-game-modal.component.html',
  styleUrls: ['./new-game-modal.component.css']
})
export class NewGameModalComponent implements OnInit {
  requestForm: FormGroup;
  @Output() new: EventEmitter<String> = new EventEmitter<String>();

  constructor(private formBuilder: FormBuilder) {
    this.requestForm = this.formBuilder.group({
      username: [''],
    });
   }

  ngOnInit(): void {}

  finish(): void {
    console.log(this.requestForm.value);
    if (this.requestForm.invalid) {
      return;
    }
    this.new.emit(this.requestForm.value.username);
  }
}
