# Database schema

`schema.sql` contains seven table definitions extracted from the project export, ordered with `users` before its dependent profile tables. Export-specific sequence values are reset. There are no data rows, accounts, password hashes, patient details, report images, or logs.

Import into a new empty MySQL 8 database using the [setup guide](../docs/SETUP.md). The original local `neusoft_db.sql` export is ignored and is not part of the public portfolio.
