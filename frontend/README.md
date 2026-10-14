# Frontend

The dashboard SPA (React, TypeScript, Vite). Its structure, conventions and build integration live
in [frontend.md](../docs/knowledge-base/frontend.md).

Entry points:

- `./verify all` at the repo root builds, tests and lints the frontend together with the rest
  ([build-run-deploy.md](../docs/knowledge-base/build-run-deploy.md), "The verify script").
- `./verify focus <frontend path>` runs the Vitest tests under that path.
- `npm run lint` in `frontend/` lints with ESLint (`eslint.config.js`) and checks Prettier
  formatting; Gradle runs it as `:frontend:npmLint`.
- `npm run build` in `frontend/` type-checks and bundles into `build/dist`; Gradle runs it as
  `:frontend:npmBuild`.
