// Netlify Function sketch: receives Quickened mirror payload, upserts into Postgres.
// Deploy only when you wire DATABASE_URL in Netlify env. App works offline without it.
import { Client } from "pg";

export async function handler(event) {
  if (event.httpMethod !== "POST") {
    return { statusCode: 405, body: "POST only" };
  }
  let data;
  try {
    data = JSON.parse(event.body || "{}");
  } catch {
    return { statusCode: 400, body: "bad json" };
  }
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();
  try {
    await client.query(
      `CREATE TABLE IF NOT EXISTS moments (
        id TEXT PRIMARY KEY,
        device_id TEXT,
        tone TEXT,
        activity TEXT,
        preview TEXT,
        duration_seconds INT,
        timestamp TIMESTAMPTZ,
        favorite BOOLEAN DEFAULT FALSE
      )`
    );
    for (const s of data.sessions || []) {
      await client.query(
        `INSERT INTO moments (id, device_id, tone, activity, preview, duration_seconds, timestamp, favorite)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
         ON CONFLICT (id) DO UPDATE SET favorite = EXCLUDED.favorite`,
        [s.id, data.device_id, s.tone, s.activity, s.preview, s.duration_seconds, s.timestamp, s.favorite]
      );
    }
    return { statusCode: 200, body: JSON.stringify({ saved: (data.sessions || []).length }) };
  } finally {
    await client.end();
  }
}
