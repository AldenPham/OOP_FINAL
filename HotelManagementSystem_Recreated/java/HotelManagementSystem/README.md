# Java Hotel Management System

Java 17+ console app organized under `hotel.model`, `hotel.service`, `hotel.data`, and `hotel.ui`.

```bash
mvn clean package
mvn exec:java
```

Run the integration smoke test using a temporary database:

```bash
mvn test-compile exec:java -Dexec.mainClass=hotel.PersistenceSmoke -Dexec.classpathScope=test
```

Maven downloads Xerial SQLite JDBC. The app creates `hotel.db` in the current working directory by default; pass a different file path as the first argument to `hotel.ui.App` when launching directly. Every accepted mutation is written to SQLite, and records are loaded with their relationships when the app restarts.
