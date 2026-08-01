#!/usr/bin/env bash
#
# Seed a fresh Gearly database from the JSON collection dumps in this directory.
#
# Each file is named gearly.<collection>.json and is imported into the matching
# collection (dropping any existing one first). Idempotent: re-running gives the
# same result.
#
# Requirements: mongoimport (MongoDB Database Tools) on PATH.
# Override the target with MONGO_URI, e.g.
#   MONGO_URI="mongodb://localhost:27017/gearly" ./seed.sh
#
# From Docker instead of a host install, prefer:  make -C backend seed
#
set -euo pipefail
cd "$(dirname "$0")"

MONGO_URI="${MONGO_URI:-mongodb://localhost:27017/gearly}"

shopt -s nullglob
files=(gearly.*.json)
if [ ${#files[@]} -eq 0 ]; then
  echo "No gearly.*.json dumps found in $(pwd)" >&2
  exit 1
fi

echo "Seeding ${#files[@]} collections into ${MONGO_URI}"
for f in "${files[@]}"; do
  coll="${f#gearly.}"; coll="${coll%.json}"
  printf '  %-14s <- %s\n' "$coll" "$f"
  mongoimport --quiet --uri "$MONGO_URI" \
    --collection "$coll" --file "$f" --jsonArray --drop
done
echo "Done."
