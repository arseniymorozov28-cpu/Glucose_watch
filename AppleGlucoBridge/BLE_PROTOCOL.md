# BLE Protocol

The iPhone app acts as a BLE peripheral.

Advertising:
- local name: `GlucoBridge`
- primary service UUID: `6F5A1000-BD38-4B9A-9A1F-8A9F2A120001`

GATT:
- characteristic UUID: `6F5A1001-BD38-4B9A-9A1F-8A9F2A120001`
- properties: `read`, `notify`
- permissions: `readable`

Payload:
- characteristic value is a JSON document in UTF-8
- the watch side can either read it on demand or subscribe to notifications

Example payload:

```json
{
  "isHigh": false,
  "isLow": false,
  "patientName": "Example Patient",
  "targetHighMmolL": 8.5,
  "targetLowMmolL": 4.0,
  "timestamp": "5/20/2026 9:25:00 PM",
  "trendArrow": 4,
  "trendMessage": "Rising",
  "trendText": "Rising",
  "valueMgDl": 126.0,
  "valueMmolL": 7.0
}
```

Stage 2 on the watch should:
- scan for `GlucoBridge`
- connect to the service
- discover the snapshot characteristic
- read the JSON payload
- subscribe to notifications for live updates
