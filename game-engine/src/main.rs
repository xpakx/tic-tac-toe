mod board;
mod minmax;
mod rabbit;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let result = rabbit::consumer().await;
    if let Err(err) = result {
        println!("{:?}", err);
    }
    loop {
    }
}
