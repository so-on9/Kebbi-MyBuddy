# Security Policy

## Supported version

Security fixes are applied to the latest version on the default branch.

## Reporting a vulnerability

Do not open a public issue containing student data, credentials, face IDs,
access tokens, or reproducible account-access details. Report the issue
privately to the project maintainer and include only the minimum information
needed to reproduce it.

## Repository rules

- Never commit `local.properties`, `.env` files, signing keys, API keys,
  database dumps, production logs, or `google-services*.json`.
- OpenAI and database credentials must remain on the PHP server in a
  root-readable environment file.
- Release builds require an HTTPS API endpoint. Cleartext HTTP is enabled only
  for local Debug builds while the legacy server is being migrated.
- Rotate a credential immediately if it is exposed in source code, an issue,
  a commit, a build artifact, or a chat transcript.
- Do not upload real student conversations, profile exports, face identifiers,
  or database backups to GitHub.

## Vendor SDK note

Android lint reports that the bundled Nuwa SDK contains a permissive X.509
trust manager. The class is inside the vendor AAR and cannot be corrected in
application source. Before a production release, obtain an updated Nuwa SDK or
written confirmation from the vendor about the affected code path. Do not use
the vendor SDK for application API or credential transport.
