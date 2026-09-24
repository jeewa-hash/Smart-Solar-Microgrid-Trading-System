# Smart Solar Microgrid Web
React + Tailwind CSS client for the C# REST API.

## Run
1. Copy `.env.example` to `.env` and set the actual API URL/port.
2. `npm install`
3. `npm run dev`
4. Open the Vite URL.

## Web scope
Backoffice: dashboard, Prosumer management, station/node management, energy slots, reservation monitoring/approval.
Grid Operator: dashboard, bookings, availability, QR verification and transfer completion.

The assignment requires the web client to communicate with the central REST API rather than MongoDB directly. Prosumer registration, booking UI, Google Maps, and QR camera scanning belong to the native Android application.
