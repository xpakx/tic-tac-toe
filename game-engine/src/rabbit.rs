use lapin::{Connection, ConnectionProperties, options::{BasicConsumeOptions, BasicAckOptions, QueueBindOptions, QueueDeclareOptions, ExchangeDeclareOptions}, types::FieldTable, message::DeliveryResult, ExchangeKind};
use serde::{Serialize, Deserialize};

use crate::{board::{Board, Symbol, Move, is_move_legal, check_win, check_draw}, minmax::min_max_decision};

const REQUEST_QUEUE: &str = "tictactoe.moves.queue";
const EXCHANGE_NAME: &str = "tictactoe.moves.topic";
const DESTINATION_EXCHANGE: &str = "tictactoe.engine.topic";

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct GameMessage {
    game_state: String,
    game_id: i32,
    column: Option<i32>,
    row: Option<i32>,
    ai: bool,
    current_symbol: String,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct EngineEvent {
    game_id: i32,
    column: i32,
    row: i32,
    new_state: String,
    #[serde(rename= "move")]
    mv: String,
    legal: bool,
    finished: bool,
    won: bool,
    drawn: bool,
}

pub async fn consumer() -> Result<(), lapin::Error> {
    let rabbit_uri = "amqp://guest:guest@localhost:5672";
    let conn = Connection::connect(&rabbit_uri, ConnectionProperties::default())
        .await
        .expect("Cannot connect to rabbitmq");

    let channel = conn.create_channel().await?;

    channel.queue_declare(
        REQUEST_QUEUE,
        QueueDeclareOptions::default(),
        Default::default(),
        )
        .await
        .expect("Cannot declare queue");

    channel
        .queue_bind(
            REQUEST_QUEUE,
            EXCHANGE_NAME,
            "move",
            QueueBindOptions::default(),
            FieldTable::default(),
            )
        .await
        .expect("Cannot bind queue");

    channel
        .exchange_declare(
            DESTINATION_EXCHANGE,
            ExchangeKind::Topic,
            ExchangeDeclareOptions {
                durable: true,
                ..Default::default()
            },
            FieldTable::default(),
            )
        .await
        .expect("Cannot declare exchange");

    let consumer = channel.basic_consume(
        REQUEST_QUEUE,
        "engine_consumer",
        BasicConsumeOptions::default(),
        FieldTable::default())
        .await
        .expect("Cannot create consumer");

    println!("Waiting for messages...");

    consumer.set_delegate({
        move |delivery: DeliveryResult| {
            println!("New message");
            let channel = channel.clone();
            async move {
                let channel = channel.clone();
                let delivery = match delivery {
                    Ok(Some(delivery)) => delivery,
                    Ok(None) => return,
                    Err(error) => {
                        println!("Failed to consume queue message {}", error);
                        return;
                    }
                };

                let message = std::str::from_utf8(&delivery.data).unwrap();
                let game_msg: GameMessage = match serde_json::from_str(message) {
                    Ok(msg) => msg,
                    Err(err) => {
                        println!("Failed to deserialize game message: {:?}", err);
                        return;
                    }
                };
                println!("Received message: {:?}", &game_msg);

                let response = process_event(&game_msg);
                println!("Response: {:?}", &response);
                let response = serde_json::to_string(&response).unwrap();

                if let Err(err) = channel
                    .basic_publish(
                        DESTINATION_EXCHANGE,
                        "engine",
                        Default::default(),
                        response.into_bytes().as_slice(),
                        Default::default(),
                        )
                        .await {
                            println!("Failed to publish message to destination exchange: {:?}", err);
                        }

                delivery
                    .ack(BasicAckOptions::default())
                    .await
                    .expect("Failed to acknowledge message");
            }
        }
    }
    );

    Ok(())
}


fn move_to_string(mv: &i32, symbol: &Symbol) -> String {
    let mut result = String::with_capacity(9);
    let mut mask = 0b100000000;
    for _ in 0..9 {
        let bit = mv & mask;
        let symbol = match bit {
            0 => '?',
            _ => match symbol {
                Symbol::X => 'X',
                Symbol::O => 'O',
            }
        };
        mask = mask >> 1;
        result.push(symbol);
    }
    result
}

impl Default for EngineEvent {
    fn default() -> EngineEvent {
        EngineEvent { 
            game_id: -1,
            column: 0,
            row: 0,
            new_state: String::from(""),
            mv: String::from(""),
            legal: false,
            finished: false,
            won: false,
            drawn: false 
        } 
    } 
}

fn process_event(game_msg: &GameMessage) -> EngineEvent {
    let board = Board::from_string(&game_msg.game_state);
    let Some(board) = board else  {
        println!("Malformed board!");
        return EngineEvent {
            game_id: game_msg.game_id, 
            ..Default::default()
        };
    };
    let bitboard = board.to_bitboard();

    println!("Before move:");
    bitboard.print();

    let symbol = match game_msg.current_symbol.as_str() {
        "x" | "X" => Symbol::X,
        _ => Symbol::O
    };

    let (mv, legal) = match game_msg.ai {
        true => (min_max_decision(&bitboard, &symbol), true),
        false => {
            let Some(row) = game_msg.row else {
                println!("Row should be present for user move");
                return EngineEvent {
                    game_id: game_msg.game_id, 
                    ..Default::default()
                };
            };
            let Some(column) = game_msg.column else {
                println!("Column should be present for user move");
                return EngineEvent {
                    game_id: game_msg.game_id, 
                    ..Default::default()
                };
            };
            let mv = Move {row: row + 1, column: column + 1};
            let mv = mv.to_bitboard();
            let Ok(mv) = mv else {
                println!("Malformed move!");
                return EngineEvent {
                    game_id: game_msg.game_id, 
                    ..Default::default()
                };
            };
            let legal = is_move_legal(&bitboard, &mv);
            (mv, legal)
        }
    };
    match legal {
        true => {
            let bitboard = bitboard.apply_move(&mv, &symbol);
            let win = check_win(&bitboard);
            let drawn = check_draw(&bitboard);

            println!("After move:");
            bitboard.print();
            if let Some(_) = win {
                println!("The game is won");
            } else if drawn {
                println!("There is a draw");
            }

            let won = match win {
                Some(_) => true,
                None => false
            };

            let mut row = 0;
            let mut column = 0;
            let mut mask = 0b100000000;
            for _ in 0..9 {
                if mask & mv != 0 {
                    break;
                }
                column += 1;
                if column >= 3 {
                    column = 0;
                    row += 1;
                }
                mask = mask >> 1;
            }

            EngineEvent {
                game_id: game_msg.game_id,
                column,
                row,
                new_state: bitboard.to_string(),
                mv: move_to_string(&mv, &symbol),
                legal,
                finished: won || drawn,
                won,
                drawn,
            }
        },
        false => {
            EngineEvent {
                game_id: game_msg.game_id,
                column: match game_msg.column {
                    Some(x) => x,
                    None => 0
                },
                row: match game_msg.row {
                    Some(x) => x,
                    None => 0
                },
                new_state: bitboard.to_string(),
                mv: move_to_string(&mv, &symbol),
                legal: false,
                finished: false,
                won: false,
                drawn: false,
            }
        }
    }
}
