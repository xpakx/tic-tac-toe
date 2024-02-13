mod board;
mod minmax;
use board::*;
use minmax::*;

fn main() {
    let b = Board {field: [
        Field::Occ(Symbol::O), Field::Empty, Field::Empty, 
        Field::Empty, Field::Occ(Symbol::X), Field::Empty,
        Field::Empty, Field::Empty, Field::Empty]};
    let bit = b.to_bitboard();

    let mv = Move{row: 1, column: 2};
    let mv = mv.to_bitboard().unwrap();
    println!("Player move");
    if is_move_legal(&bit, &mv) {
        let bit = bit.apply_move(&mv, &Symbol::O);
        bit.print();
        let ai_move =  min_max_decision(&bit, &Symbol::X);
        let bit = bit.apply_move(&ai_move, &Symbol::O);
        println!("AI move");
        bit.print();
    } else {
        println!("Move illegal!");
    }
}
