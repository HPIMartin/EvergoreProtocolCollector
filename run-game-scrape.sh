#!/usr/bin/env bash
set -euo pipefail
credentials="${EVERGORE_CREDENTIALS_FILE:-$(dirname "$(git rev-parse --git-common-dir)")/zugang.txt}"
[ -r "$credentials" ] || { echo "no readable credentials file at $credentials" >&2; exit 1; }
export EVERGORE_CREDENTIALS_USERNAME="$(sed -n 1p "$credentials" | tr -d '\r')"
export EVERGORE_CREDENTIALS_PASSWORD="$(sed -n 2p "$credentials" | tr -d '\r')"
./gradlew --no-daemon test --rerun --tests "*GameCatalogScrapeCheck*" -DgameCatalog.scrape=true "$@"
