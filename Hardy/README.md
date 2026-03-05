This folder# Digital Wallet & Transaction Engine

A mini digital wallet system built with pure Java — no Spring, no database.  
All data is stored in-memory. Built as a Java Core assignment demonstrating OOP, functional programming, and clean architecture.

---

## How to Build and Run

```bash
# Build the project
mvn clean compile

# Run the demo
mvn exec:java -Dexec.mainClass="wallet.Main"

# Run all tests
mvn test
```

---

## Project Structure

```
src/
├── main/java/wallet/
│   ├── error/
│   │   └── DomainError.java          ← All possible errors in one enum
│   ├── model/
│   │   ├── Currency.java             ← Supported currencies (USD, IQD)
│   │   ├── Transaction.java          ← Immutable record of one financial event
│   │   ├── TransactionStatus.java    ← SUCCESS / FAILED
│   │   ├── TransactionType.java      ← DEPOSIT / WITHDRAWAL / TRANSFER
│   │   └── Wallet.java               ← Wallet with balance operations
│   ├── service/
│   │   └── WalletService.java        ← All business logic
│   └── Main.java                     ← Demo runner
│
└── test/java/wallet/service/
    └── WalletServiceTest.java        ← JUnit 5 tests
```

---

## Features

- Create wallets with a name and currency
- Deposit and withdraw money
- Transfer money between wallets
- View transaction history per wallet
- View all transactions
- Full error handling for every invalid scenario

---

## Design Decisions

**Why `BigDecimal` instead of `double`?**  
`double` has floating point precision errors — for example `0.1 + 0.2` in double gives `0.30000000000000004`. Money must always be exact, so `BigDecimal` is the only correct choice.

**Why Vavr `Either` instead of throwing exceptions?**  
`Either<DomainError, T>` makes every method's possible failures visible in the return type itself. The caller is forced to handle both success and failure. With exceptions, errors are invisible until runtime and easy to forget to handle. This is a functional programming approach — errors are values, not surprises.

**Why `Option` for wallet lookup?**  
`Option<Wallet>` forces handling the "not found" case instead of getting a silent `NullPointerException`. It chains cleanly into `Either` via `.toEither(DomainError.WALLET_NOT_FOUND)`.

**Why `DomainError` enum instead of multiple exception classes?**  
All possible errors live in one place. Easy to add new ones, easy to see every error the system can produce, and they work naturally as the left side of `Either`.

**Why `ConcurrentHashMap` and `synchronizedList`?**  
The concurrent transfer test runs multiple threads simultaneously. `ConcurrentHashMap` allows thread-safe reads and writes to the wallet store without locking the whole map. `Collections.synchronizedList` protects the transaction list from concurrent modification.

**Why `synchronized` on `deposit`, `withdraw`, and `transfer`?**  
Even with a thread-safe map, the balance update itself (read → modify → write) is not atomic. `synchronized` on these methods ensures only one thread can modify a wallet's balance at a time, preventing race conditions and corrupted balances.

**Why immutable `Transaction`?**  
Once a transaction is recorded it should never change. All fields are `final` and set in the constructor. This makes transaction history trustworthy — no one can accidentally modify a past transaction.

---

## Error Handling

All errors are represented as `DomainError` enum values and returned as `Either.left(...)` — no exceptions are thrown for business logic failures.

| Error | When It Occurs |
|-------|---------------|
| `WALLET_NOT_FOUND` | The given wallet ID does not exist |
| `INVALID_AMOUNT` | Amount is null, zero, or negative |
| `INSUFFICIENT_BALANCE` | Withdrawal or transfer exceeds current balance |
| `SELF_TRANSFER` | Transfer source and destination are the same wallet |
| `CURRENCY_MISMATCH` | Transfer attempted between wallets of different currencies |

---

## Tests

All tests are in `WalletServiceTest.java` and cover:

| Test | What It Verifies |
|------|-----------------|
| `shouldTransferSuccessfully` | Balances update correctly after a valid transfer |
| `shouldFailWhenInsufficientBalance` | Transfer fails when sender does not have enough funds |
| `shouldFailOnSelfTransfer` | Transfer to the same wallet is rejected |
| `shouldHandleConcurrentTransfersSafely` | 5 threads transferring simultaneously produce correct final balances |

---

## Dependencies

| Library | Purpose |
|---------|---------|
| Vavr 0.10.4 | `Either`, `Option` for functional error handling |
| Lombok 1.18.36 | `@Getter` to reduce boilerplate |
| SLF4J + Logback | Logging across all service operations |
| JUnit 5 | Unit and concurrency testing |
| AssertJ | Fluent assertions in tests | is specifically created for hardy to Share all his files and projects.
