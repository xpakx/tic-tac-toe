fn main() {
    let b = Board {field: [
        Field::Occ(Symbol::O), Field::Empty, Field::Empty, 
        Field::Empty, Field::Occ(Symbol::X), Field::Empty,
        Field::Empty, Field::Empty, Field::Empty]};
    for a in b.field {
        if let Field::Occ(a) = a {
            if a == Symbol::X {
                print!("X");
            } else {
                print!("O");
            }
        } else {
            print!("_");
        }
    }

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
