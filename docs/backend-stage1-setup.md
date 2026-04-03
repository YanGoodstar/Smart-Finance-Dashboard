# Backend Stage 1 Setup

## Runtime assumptions

- Java 17
- Maven 3.8+
- MySQL 8+
- Execute the schema script manually before starting the application

## Default database settings

- Database: `smart_finance_dashboard`
- Host: `localhost`
- Port: `3306`
- Username: `root`
- Password: `123456`

These defaults can be overridden with:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

## Start commands

Initialize a brand new database schema with:

```sql
source sql/init_schema.sql;
source sql/manual_update_v1.sql;
```

If your local database was already initialized from the old baseline and you
only need this round's incremental changes, execute:

```sql
source sql/manual_update_v1.sql;
```

Run tests:

```powershell
mvn test
```

Run the application:

```powershell
mvn spring-boot:run
```
