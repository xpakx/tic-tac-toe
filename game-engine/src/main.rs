mod board;
use board::*;

fn main() {
    let b = Board {field: [
        Field::Occ(Symbol::O), Field::Empty, Field::Empty, 
        Field::Empty, Field::Occ(Symbol::X), Field::Empty,
        Field::Empty, Field::Empty, Field::Empty]};
    for a in &b.field {
        if let Field::Occ(a) = a {
            if a == &Symbol::X {
                print!("X");
            } else {
                print!("O");
            }
        } else {
            print!("_");
        }
    }
    println!("");
    let bit = b.to_bitboard();
    println!("{:09b}", bit.x);
    println!("{:09b}", bit.o);

    let mv = Move{row: 1, column: 2};
    let mv = mv.to_bitboard().unwrap();
    println!("{:09b}", mv);


    println!("{}", is_move_legal(&bit, &mv));
    println!("{:?}", check_win(&bit));
}
