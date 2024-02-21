use lapin::{Connection, ConnectionProperties, options::{BasicConsumeOptions, BasicAckOptions, QueueBindOptions, QueueDeclareOptions}, types::FieldTable, message::DeliveryResult};
use serde::{Serialize, Deserialize};

const REQUEST_QUEUE: &str = "tictactoe.moves.queue";
const EXCHANGE_NAME: &str = "tictactoe.moves.topic";

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct GameMessage {
    game_state: String,
    game_id: i32,
    column: i32,
    row: i32,
    ai: bool,
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
            async move {
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

                println!("Received message: {:?}", game_msg);

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
