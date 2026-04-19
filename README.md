# mayphus-map

Standalone browser map project extracted from `mayphus-sites`.

It keeps the focused Hangzhou macro-road presentation, but now lives as its own small ClojureScript app under `~/making/mayphus-map`.

## Commands

```bash
bun install
bun run dev
bun run build
bunx wrangler deploy --config wrangler.jsonc --env production
```

`bun run dev` starts Shadow CLJS and serves the app at [http://localhost:8792](http://localhost:8792).

## Cloudflare Deploy

`wrangler.jsonc` is configured to publish the built `dist/` folder as a static-assets Worker:

- production custom domain: `map.mayphus.org`
- preview environment: `mayphus-map-preview.workers.dev`

GitHub Actions deploys preview branches (`codex/**`, `feature/**`, `feat/**`) and production on `main`.
Set these repository secrets before enabling the workflow:

- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`
