import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'field'
})
export class FieldPipe implements PipeTransform {

  transform(value: String): String {
    if (value == "X") {
      return "✘"
    }
    if (value == "O") {
      return "⭘"
    }
    return "";
  }

}
