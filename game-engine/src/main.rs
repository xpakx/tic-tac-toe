mod board;
mod minmax;
mod rabbit;
use std::env;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let rabbit_env = env::var("RABBIT_URL");
    let rabbit_uri = match rabbit_env {
        Ok(env) => env,
        _ => String::from("amqp://guest:guest@localhost:5672")
    };
    let result = rabbit::consumer(&rabbit_uri).await;
    if let Err(err) = result {
        println!("{:?}", err);
    }
    loop {
    }
}
