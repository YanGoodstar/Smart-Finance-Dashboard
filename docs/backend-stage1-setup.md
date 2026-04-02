# Backend Stage 1 Setup

## Runtime assumptions

- Java 17
- Maven 3.8+
- MySQL 8+

## Default database settings

- Database: `smart_finance_dashboard`
- Host: `localhost`
- Port: `3306`
- Username: `root`
- Password: `root`

These defaults can be overridden with:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

## Start commands

Run tests:

```powershell
mvn test
```

Run the application:

```powershell
mvn spring-boot:run
```
