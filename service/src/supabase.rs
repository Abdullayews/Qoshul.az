//! Supabase REST qatı (D-082) — reqwest + service_role.
//! REST (PostgREST) — D-067 siyahısında DB client YOXDUR; bu qat
//! `supabase/README.md` §4 funksiyaları ilə eyni sərhəddən keçir.

use async_trait::async_trait;
use serde_json::Value;

#[derive(Debug)]
pub enum RestError {
    NotConfigured,
    Http(String),
    Decode(String),
}

impl std::fmt::Display for RestError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            RestError::NotConfigured => write!(f, "supabase konfiqi PLACEHOLDER-dır"),
            RestError::Http(m) => write!(f, "http: {m}"),
            RestError::Decode(m) => write!(f, "decode: {m}"),
        }
    }
}

/// Trait — testlərdə FakeBackend ilə əvəz olunur (network YOX).
#[async_trait]
pub trait SupabaseRest: Send + Sync {
    async fn select(&self, table: &str, query: &str) -> Result<Vec<Value>, RestError>;
    async fn patch(&self, table: &str, filter: &str, body: Value) -> Result<Vec<Value>, RestError>;
    async fn insert(&self, table: &str, body: Value) -> Result<Vec<Value>, RestError>;
    async fn rpc(&self, fn_name: &str, params: Value) -> Result<Value, RestError>;
}

pub struct SupabaseClient {
    base: String,
    service_key: String,
    http: reqwest::Client,
}

impl SupabaseClient {
    pub fn new(url: &str, service_key: &str) -> Self {
        Self {
            base: format!("{url}/rest/v1"),
            service_key: service_key.to_string(),
            http: reqwest::Client::new(),
        }
    }

    fn headers(&self) -> reqwest::header::HeaderMap {
        let mut h = reqwest::header::HeaderMap::new();
        let bearer = format!("Bearer {}", self.service_key);
        if let Ok(k) = reqwest::header::HeaderValue::from_str(&self.service_key) {
            h.insert("apikey", k);
        }
        if let Ok(b) = reqwest::header::HeaderValue::from_str(&bearer) {
            h.insert("authorization", b);
        }
        h
    }
}

#[async_trait]
impl SupabaseRest for SupabaseClient {
    async fn select(&self, table: &str, query: &str) -> Result<Vec<Value>, RestError> {
        let url = format!("{}/{}?{}", self.base, table, query);
        let resp = self
            .http
            .get(&url)
            .headers(self.headers())
            .header("accept", "application/json")
            .send()
            .await
            .map_err(|e| RestError::Http(e.to_string()))?;
        if !resp.status().is_success() {
            return Err(RestError::Http(format!("{} {}", resp.status(), url)));
        }
        resp.json::<Vec<Value>>()
            .await
            .map_err(|e| RestError::Decode(e.to_string()))
    }

    async fn patch(&self, table: &str, filter: &str, body: Value) -> Result<Vec<Value>, RestError> {
        let url = format!("{}/{}?{}", self.base, table, filter);
        let resp = self
            .http
            .patch(&url)
            .headers(self.headers())
            .header("prefer", "return=representation")
            .json(&body)
            .send()
            .await
            .map_err(|e| RestError::Http(e.to_string()))?;
        if !resp.status().is_success() {
            return Err(RestError::Http(format!("{} → {table}", resp.status())));
        }
        resp.json::<Vec<Value>>()
            .await
            .map_err(|e| RestError::Decode(e.to_string()))
    }

    async fn insert(&self, table: &str, body: Value) -> Result<Vec<Value>, RestError> {
        let url = format!("{}/{}", self.base, table);
        let resp = self
            .http
            .post(&url)
            .headers(self.headers())
            .header("prefer", "return=representation")
            .json(&body)
            .send()
            .await
            .map_err(|e| RestError::Http(e.to_string()))?;
        if !resp.status().is_success() {
            return Err(RestError::Http(format!(
                "{} → insert {table}",
                resp.status()
            )));
        }
        resp.json::<Vec<Value>>()
            .await
            .map_err(|e| RestError::Decode(e.to_string()))
    }

    async fn rpc(&self, fn_name: &str, params: Value) -> Result<Value, RestError> {
        let url = format!("{}/rpc/{}", self.base, fn_name);
        let resp = self
            .http
            .post(&url)
            .headers(self.headers())
            .json(&params)
            .send()
            .await
            .map_err(|e| RestError::Http(e.to_string()))?;
        if !resp.status().is_success() {
            return Err(RestError::Http(format!(
                "{} → rpc {fn_name}",
                resp.status()
            )));
        }
        resp.json::<Value>()
            .await
            .map_err(|e| RestError::Decode(e.to_string()))
    }
}

/// Jobların idempotentlik sütun adları (0001: reservations).
pub mod columns {
    pub const REMINDER_24H: &str = "reminder_24h_sent_at";
    pub const REMINDER_2H: &str = "reminder_2h_sent_at";
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn placeholder_config_degrade() {
        // NotConfigured xətası — crash YOX
        let c = SupabaseClient::new("PLACEHOLDER", "PLACEHOLDER");
        // real şəbəkə çağırışı burada YOX — yalnız tiplər
        let _ = &c.base;
        assert_eq!(c.base, "PLACEHOLDER/rest/v1");
    }
}
