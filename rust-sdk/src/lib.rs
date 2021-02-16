use std::sync::{Arc, Mutex};

use tracing_subscriber::{fmt::MakeWriter, EnvFilter};

mod error;
mod machine;

pub use error::*;
pub use machine::{Device, DeviceLists, OlmMachine, Request, RequestType, Sas};

pub trait Logger: Send {
    fn log(&self, log_line: String);
}

impl std::io::Write for LoggerWrapper {
    fn write(&mut self, buf: &[u8]) -> std::io::Result<usize> {
        let data = String::from_utf8_lossy(buf).to_string();
        self.inner.lock().unwrap().log(data);

        Ok(buf.len())
    }

    fn flush(&mut self) -> std::io::Result<()> {
        Ok(())
    }
}

impl MakeWriter for LoggerWrapper {
    type Writer = LoggerWrapper;

    fn make_writer(&self) -> Self::Writer {
        self.clone()
    }
}

#[derive(Clone)]
pub struct LoggerWrapper {
    inner: Arc<Mutex<Box<dyn Logger>>>,
}

pub fn set_logger(logger: Box<dyn Logger>) {
    let logger = LoggerWrapper {
        inner: Arc::new(Mutex::new(logger)),
    };

    let filter = EnvFilter::from_default_env().add_directive(
        "matrix_sdk_crypto=trace"
            .parse()
            .expect("Can't parse logging filter directive"),
    );

    let _ = tracing_subscriber::fmt()
        .with_writer(logger)
        .with_env_filter(filter)
        .without_time()
        .try_init();
}

include!(concat!(env!("OUT_DIR"), "/olm.uniffi.rs"));
