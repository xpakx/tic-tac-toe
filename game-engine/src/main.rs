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
    println!("{:09b}", mv.to_bitboard().unwrap());
    
}

#[derive(PartialEq, Eq)]
enum Symbol {
    X,
    O
}

enum Field {
    Empty,
    Occ(Symbol)
}

struct Board {
    field: [Field; 9],
}

struct BitBoard {
    x: i32,
    o: i32
}

impl Board{
    fn to_bitboard(&self) -> BitBoard {
        let mut bitboard_x = 0;
        let mut bitboard_o = 0;
        for a in &self.field {
            bitboard_x = bitboard_x << 1;
            bitboard_o = bitboard_o << 1;
            if let Field::Occ(a) = a {
                if a == &Symbol::X {
                    bitboard_x += 1;
                } else {
                    bitboard_o += 1;
                }
            } 
        }
        return BitBoard {x: bitboard_x, o: bitboard_o};
    }
}

struct Move {
    row: i32,
    column: i32
}

impl Move {
    fn to_bitboard(&self) -> Result<i32, &str> {
        let mut bitboard = 0;
        if self.row <= 0 || self.column <= 0 || self.row > 9 || self.column > 9 {
            return Err("Outside of board");
        }
        let row_offset = match self.row {
            1 => 0,
            2 => 3,
            3 => 6,
            _ => panic!("Invalid row")
        };
        let position = row_offset + (self.column-1);
        println!("{}", row_offset);
        println!("{}", position);
        bitboard = bitboard << position;
        bitboard += 1;
        bitboard = bitboard << 9-position-1;
        return Ok(bitboard);
    }
}
