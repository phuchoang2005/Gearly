# Seed data

MongoDB collection dumps (exported as JSON) used to seed a fresh database.
Relocated here from `src/main/java/com/dominator/bookify/data/` so they no
longer sit inside the Java source tree.

Kept in-repo intentionally: Sprint 5 (the Bookify→Gearly / Book→Product
rename) re-seeds the database from these dumps and commits a repeatable
migration script. Do not delete without updating that plan.

Import example (adjust db/host as needed):

```bash
for f in bookify.*.json; do
  coll="$(basename "$f" .json | sed 's/^bookify\.//')"
  mongoimport --uri "mongodb://localhost:27017/bookify" \
    --collection "$coll" --file "$f" --jsonArray
done
```
