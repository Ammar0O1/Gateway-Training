package wallet;

import io.vavr.control.Either;
import wallet.domain.*;
import wallet.error.WalletError;
import wallet.repository.TransactionRepository;
import wallet.repository.WalletRepository;
import wallet.service.TransactionService;
import wallet.service.WalletService;

import java.math.BigDecimal;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Digital Wallet System Demo ===\n");

        // Step 1: Initialize repositories and services
        WalletRepository walletRepo = new WalletRepository();
        TransactionRepository transactionRepo = new TransactionRepository();
        WalletService walletService = new WalletService(walletRepo, transactionRepo);
        TransactionService transactionService = new TransactionService(walletRepo, transactionRepo);

        // Step 2: Create wallets
        System.out.println("Creating wallets...");
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Wallet bob = walletService.createWallet("Bob", CurrencyType.USD);
        Wallet charlie = walletService.createWallet("Charlie", CurrencyType.EUR);

        System.out.println("Created wallet for " + alice.getOwnerName() + " - ID: " + alice.getId());
        System.out.println("Created wallet for " + bob.getOwnerName() + " - ID: " + bob.getId());
        System.out.println("Created wallet for " + charlie.getOwnerName() + " - ID: " + charlie.getId());
        System.out.println();

        // Step 3: Deposit money
        System.out.println("Depositing money...");
        Money deposit100 = new Money(new BigDecimal("100.00"), CurrencyType.USD);
        Money deposit200 = new Money(new BigDecimal("200.00"), CurrencyType.USD);
        Money deposit150 = new Money(new BigDecimal("150.00"), CurrencyType.EUR);

        walletService.deposit(alice.getId(), deposit100);
        walletService.deposit(bob.getId(), deposit200);
        walletService.deposit(charlie.getId(), deposit150);

        System.out.println("Alice deposited $100");
        System.out.println("Bob deposited $200");
        System.out.println("Charlie deposited €150");
        System.out.println();

        // Step 4: Check balances
        System.out.println("Current balances:");
        walletService.getBalance(alice.getId())
                .peek(balance -> System.out.println("Alice: " + balance));
        walletService.getBalance(bob.getId())
                .peek(balance -> System.out.println("Bob: " + balance));
        walletService.getBalance(charlie.getId())
                .peek(balance -> System.out.println("Charlie: " + balance));
        System.out.println();

        // Step 5: Transfer money
        System.out.println("Transferring $50 from Alice to Bob...");
        Money transfer50 = new Money(new BigDecimal("50.00"), CurrencyType.USD);
        Either<WalletError, Transaction> transferResult =
                transactionService.transfer(alice.getId(), bob.getId(), transfer50);

        transferResult
                .peek(tx -> System.out.println("Transfer successful! Transaction ID: " + tx.getId()))
                .peekLeft(error -> System.out.println("Transfer failed: " + error.getMessage()));
        System.out.println();

        // Step 6: Check balances after transfer
        System.out.println("Balances after transfer:");
        walletService.getBalance(alice.getId())
                .peek(balance -> System.out.println("Alice: " + balance));
        walletService.getBalance(bob.getId())
                .peek(balance -> System.out.println("Bob: " + balance));
        System.out.println();

        // Step 7: View transaction history
        System.out.println("Alice's transaction history:");
        List<Transaction> aliceHistory = transactionService.getHistory(alice.getId());
        aliceHistory.forEach(tx ->
                System.out.println("  " + tx.getType() + " - " + tx.getAmount() + " - " + tx.getStatus())
        );
        System.out.println();

        // Step 8: Demo error cases
        System.out.println("=== Testing Error Scenarios ===\n");

        // Try to withdraw more than balance
        System.out.println("Attempting to withdraw $200 from Alice (only has $50)...");
        Money withdraw200 = new Money(new BigDecimal("200.00"), CurrencyType.USD);
        walletService.withdraw(alice.getId(), withdraw200)
                .peek(tx -> System.out.println("Withdrawal successful"))
                .peekLeft(error -> System.out.println("Error: " + error.getMessage()));
        System.out.println();

        // Try to transfer between different currencies
        System.out.println("Attempting to transfer $50 from Alice to Charlie (different currencies)...");
        transactionService.transfer(alice.getId(), charlie.getId(), transfer50)
                .peek(tx -> System.out.println("Transfer successful"))
                .peekLeft(error -> System.out.println("Error: " + error.getMessage()));
        System.out.println();

        // Try self-transfer
        System.out.println("Attempting to transfer to same wallet...");
        transactionService.transfer(alice.getId(), alice.getId(), transfer50)
                .peek(tx -> System.out.println("Transfer successful"))
                .peekLeft(error -> System.out.println("Error: " + error.getMessage()));
        System.out.println();

        System.out.println("=== Demo Complete ===");
    }
}