# Your own bank data goes here, and only here

Everything in this folder except this README is ignored by git, so your files stay on your
computer. A check (`PrivacyTest`) fails if anything here ever gets tracked.

1. Download a CSV export from your bank's website (most banks call it "Download" or "Export").
2. Copy `data/private-example/seagrape.properties` into this folder and rename it, for example `mybank.properties`.
3. Put your CSV next to it and edit the profile so it matches your file's columns.
4. Run `./run structure --data private`, then `./mvnw test -Dweek=2 -Ddata=private`.

The output goes to `out/`, which git also ignores. `./run site` refuses to publish it.
The AI step never sees your rows or account names, only the computed facts, and only if
you add `--send` after reading `out/ai_request_preview.json`.
