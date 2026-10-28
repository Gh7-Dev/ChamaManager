# ChamaManager

ChamaManager is a Java desktop application for managing a chama savings group's members, contributions, loans, repayments, and reports. It uses a Swing-based interface and a MySQL database.

## Features represented in the source tree

- Member and treasurer-oriented dashboards
- Member management
- Recording contributions
- Loan management and repayment recording
- Reports for group activity
- DAO and service layers separating data access and application logic
- Custom exceptions for invalid login, duplicate contributions, insufficient savings, repayment validation, loan state, and data-access errors

## Technology

- Java 21
- Java Swing
- MySQL
- JDBC
- Apache Ant / NetBeans project structure

## Architecture

The source is organized under the `chamamanager` package:

```text
src/chamamanager/
├── dao/             Data-access classes
├── exceptions/      Domain and data-access exceptions
├── logic/           Application services
├── model/           User, member, contribution, loan, repayment models
├── presentation/    Swing frames, dashboards, and panels
└── util/            Database connection and password hashing utilities
```

## Database setup

1. Install MySQL and create a database for the application.
2. Review and execute `database/chama_manager.sql`.
3. Copy `db.properties.example` to `db.properties` and fill in your local database settings. Keep real credentials out of version control.
4. Ensure the MySQL Connector/J JAR is available to the project. The repository currently includes a connector JAR under `lib/`.

Check the database schema and connection utility for the exact database name and property keys before running the application.

## Open and run

Open the project in NetBeans using the existing Ant project files. The project targets Java 21 and defines `chamamanager.Main` as its main class. Configure the database before launching the application.

## Project context

This is a collaborative academic software project. The implementation demonstrates object-oriented design, separation of concerns, Swing GUI development, JDBC data access, and relational database integration.

## Current scope

The repository contains the application source and SQL schema. Confirm the current application flow and test it against a local MySQL instance before describing it as production-ready.
