# NeuroPACS frontend

Vue 3 and Three.js interface for patient, doctor, nurse/technician, and admin workflows. Includes brain and spleen analysis pages, imaging viewers, report editing, and local AI chat components.

See [setup](../docs/SETUP.md), [architecture](../docs/ARCHITECTURE.md), and [known limitations](../docs/LIMITATIONS.md).

`src/config.js` centralizes public API settings. Copy `.env.example` to `.env.local` to customize URLs. Do not put credentials in Vite variables.

Requires Node.js `^20.19.0` or `>=22.12.0`. Use `npm ci` for dependencies. `npm run dev` starts the development UI; `npm run build` builds it. Neither was executed during portfolio preparation.
