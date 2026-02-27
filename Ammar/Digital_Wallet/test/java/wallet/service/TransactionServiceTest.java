package wallet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.vavr.control.Either;
import wallet.domain.*;
import wallet.error.*;
import wallet.repository.*;
import java.util.UUID;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private WalletRepository walletRepository;
    private TransactionRepository transactionRepository;
    private WalletService walletService;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        walletRepository = new WalletRepository();
        transactionRepository = new TransactionRepository();
        walletService = new WalletService(walletRepository, transactionRepository);
        transactionService = new TransactionService(walletRepository, transactionRepository);
    }

    @Test
    void testSuccessfulTransfer() {
        // ARRANGE - Create two wallets with money
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Wallet bob = walletService.createWallet("Bob", CurrencyType.USD);

        Money deposit100 = new Money(new BigDecimal("100.00"), CurrencyType.USD);
        walletService.deposit(alice.getId(), deposit100);

        Money transfer50 = new Money(new BigDecimal("50.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                transactionService.transfer(alice.getId(), bob.getId(), transfer50);

        // ASSERT
        assertTrue(result.isRight(), "Transfer should succeed");

        // Check Alice's balance (100 - 50 = 50)
        Either<WalletError, Money> aliceBalance = walletService.getBalance(alice.getId());
        assertEquals(new BigDecimal("50.00"), aliceBalance.get().getAmount());

        // Check Bob's balance (0 + 50 = 50)
        Either<WalletError, Money> bobBalance = walletService.getBalance(bob.getId());
        assertEquals(new BigDecimal("50.00"), bobBalance.get().getAmount());
    }

    @Test
    void testTransferInsufficientBalance() {
        // ARRANGE
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Wallet bob = walletService.createWallet("Bob", CurrencyType.USD);

        Money deposit30 = new Money(new BigDecimal("30.00"), CurrencyType.USD);
        walletService.deposit(alice.getId(), deposit30);

        Money transfer50 = new Money(new BigDecimal("50.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                transactionService.transfer(alice.getId(), bob.getId(), transfer50);

        // ASSERT
        assertTrue(result.isLeft(), "Transfer should fail - insufficient balance");
    }

    @Test
    void testTransferToSameWallet() {
        // ARRANGE
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Money deposit100 = new Money(new BigDecimal("100.00"), CurrencyType.USD);
        walletService.deposit(alice.getId(), deposit100);

        Money transfer50 = new Money(new BigDecimal("50.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                transactionService.transfer(alice.getId(), alice.getId(), transfer50);

        // ASSERT
        assertTrue(result.isLeft(), "Self-transfer should fail");
    }

    @Test
    void testTransferCurrencyMismatch() {
        // ARRANGE
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Wallet charlie = walletService.createWallet("Charlie", CurrencyType.EUR);

        Money deposit100 = new Money(new BigDecimal("100.00"), CurrencyType.USD);
        walletService.deposit(alice.getId(), deposit100);

        Money transfer50 = new Money(new BigDecimal("50.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                transactionService.transfer(alice.getId(), charlie.getId(), transfer50);

        // ASSERT
        assertTrue(result.isLeft(), "Transfer should fail - currency mismatch");
    }

    @Test
    void testGetHistory() {
        // ARRANGE
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Money deposit100 = new Money(new BigDecimal("100.00"), CurrencyType.USD);
        Money withdraw30 = new Money(new BigDecimal("30.00"), CurrencyType.USD);

        // ACT
        walletService.deposit(alice.getId(), deposit100);
        walletService.withdraw(alice.getId(), withdraw30);

        List<Transaction> history = transactionService.getHistory(alice.getId());

        // ASSERT - Check count
        assertEquals(2, history.size(), "Should have 2 transactions");

        // Count each type (don't care about order)
        long deposits = history.stream()
                .filter(tx -> tx.getType() == TransactionType.DEPOSIT)
                .count();
        long withdraws = history.stream()
                .filter(tx -> tx.getType() == TransactionType.WITHDRAW)
                .count();

        assertEquals(1, deposits, "Should have 1 deposit");
        assertEquals(1, withdraws, "Should have 1 withdraw");
    }

    @Test
    void testGetHistoryByType() {
        // ARRANGE
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Wallet bob = walletService.createWallet("Bob", CurrencyType.USD);

        Money deposit100 = new Money(new BigDecimal("100.00"), CurrencyType.USD);
        Money deposit50 = new Money(new BigDecimal("50.00"), CurrencyType.USD);
        Money transfer30 = new Money(new BigDecimal("30.00"), CurrencyType.USD);

        // ACT - Do mixed transactions
        walletService.deposit(alice.getId(), deposit100);
        walletService.deposit(alice.getId(), deposit50);
        transactionService.transfer(alice.getId(), bob.getId(), transfer30);

        List<Transaction> deposits = transactionService.getHistoryByType(
                alice.getId(),
                TransactionType.DEPOSIT
        );

        // ASSERT
        assertEquals(2, deposits.size(), "Should have 2 deposits");
        deposits.forEach(tx ->
                assertEquals(TransactionType.DEPOSIT, tx.getType())
        );
    }

    @Test
    void testGetHistoryByStatus() {
        // ARRANGE
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Money deposit100 = new Money(new BigDecimal("100.00"), CurrencyType.USD);

        // ACT
        walletService.deposit(alice.getId(), deposit100);

        List<Transaction> successfulTx = transactionService.getHistoryByStatus(
                alice.getId(),
                TransactionStatus.SUCCESS
        );

        // ASSERT
        assertEquals(1, successfulTx.size(), "Should have 1 successful transaction");
        assertEquals(TransactionStatus.SUCCESS, successfulTx.get(0).getStatus());
    }

    @Test
    void testTransferExactBalance() {
        // ARRANGE - Critical: transfer entire balance
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Wallet bob = walletService.createWallet("Bob", CurrencyType.USD);

        Money deposit = new Money(new BigDecimal("100.00"), CurrencyType.USD);
        walletService.deposit(alice.getId(), deposit);

        Money transferAll = new Money(new BigDecimal("100.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                transactionService.transfer(alice.getId(), bob.getId(), transferAll);

        // ASSERT
        assertTrue(result.isRight(), "Should be able to transfer exact balance");

        // Verify Alice has zero
        Either<WalletError, Money> aliceBalance = walletService.getBalance(alice.getId());
        assertEquals(
                0,
                aliceBalance.get().getAmount().compareTo(BigDecimal.ZERO),
                "Alice should have zero balance"
        );

        // Verify Bob has all the money
        Either<WalletError, Money> bobBalance = walletService.getBalance(bob.getId());
        assertEquals(
                0,
                bobBalance.get().getAmount().compareTo(new BigDecimal("100.00")),
                "Bob should have 100.00"
        );
    }

    @Test
    void testTransferFromNonExistentWallet() {
        // ARRANGE
        UUID nonExistentId = UUID.randomUUID();
        Wallet bob = walletService.createWallet("Bob", CurrencyType.USD);
        Money amount = new Money(new BigDecimal("50.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                transactionService.transfer(nonExistentId, bob.getId(), amount);

        // ASSERT
        assertTrue(result.isLeft(), "Transfer from non-existent wallet should fail");
    }

    @Test
    void testTransferToNonExistentWallet() {
        // ARRANGE
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        walletService.deposit(alice.getId(), new Money(new BigDecimal("100.00"), CurrencyType.USD));

        UUID nonExistentId = UUID.randomUUID();
        Money amount = new Money(new BigDecimal("50.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                transactionService.transfer(alice.getId(), nonExistentId, amount);

        // ASSERT
        assertTrue(result.isLeft(), "Transfer to non-existent wallet should fail");
    }

    @Test
    void testGetHistoryEmptyWallet() {
        // ARRANGE
        Wallet wallet = walletService.createWallet("EmptyUser", CurrencyType.USD);

        // ACT
        List<Transaction> history = transactionService.getHistory(wallet.getId());

        // ASSERT
        assertTrue(history.isEmpty(), "New wallet should have empty history");
    }

    @Test
    void testMultipleTransfersBetweenSameWallets() {
        // ARRANGE
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Wallet bob = walletService.createWallet("Bob", CurrencyType.USD);

        walletService.deposit(alice.getId(), new Money(new BigDecimal("1000.00"), CurrencyType.USD));

        Money transfer = new Money(new BigDecimal("100.00"), CurrencyType.USD);

        // ACT - Do 5 transfers
        transactionService.transfer(alice.getId(), bob.getId(), transfer);
        transactionService.transfer(alice.getId(), bob.getId(), transfer);
        transactionService.transfer(alice.getId(), bob.getId(), transfer);
        transactionService.transfer(alice.getId(), bob.getId(), transfer);
        transactionService.transfer(alice.getId(), bob.getId(), transfer);

        // ASSERT
        Either<WalletError, Money> aliceBalance = walletService.getBalance(alice.getId());
        Either<WalletError, Money> bobBalance = walletService.getBalance(bob.getId());

        assertEquals(
                0,
                aliceBalance.get().getAmount().compareTo(new BigDecimal("500.00")),
                "Alice should have 500.00 left"
        );

        assertEquals(
                0,
                bobBalance.get().getAmount().compareTo(new BigDecimal("500.00")),
                "Bob should have 500.00"
        );
    }

    @Test
    void testTransferZeroAmount() {
        // ARRANGE
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Wallet bob = walletService.createWallet("Bob", CurrencyType.USD);

        walletService.deposit(alice.getId(), new Money(new BigDecimal("100.00"), CurrencyType.USD));

        Money zeroAmount = new Money(BigDecimal.ZERO, CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                transactionService.transfer(alice.getId(), bob.getId(), zeroAmount);

        // ASSERT
        assertTrue(result.isLeft(), "Transfer of zero amount should fail");
    }

    @Test
    void testMoneyConservationAfterMultipleOperations() {
        // ARRANGE - Critical banking test!
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Wallet bob = walletService.createWallet("Bob", CurrencyType.USD);
        Wallet charlie = walletService.createWallet("Charlie", CurrencyType.USD);

        Money initial = new Money(new BigDecimal("1000.00"), CurrencyType.USD);
        walletService.deposit(alice.getId(), initial);
        walletService.deposit(bob.getId(), initial);
        walletService.deposit(charlie.getId(), initial);

        BigDecimal totalBefore = new BigDecimal("3000.00");

        // ACT - Do various operations
        transactionService.transfer(alice.getId(), bob.getId(), new Money(new BigDecimal("100.00"), CurrencyType.USD));
        transactionService.transfer(bob.getId(), charlie.getId(), new Money(new BigDecimal("200.00"), CurrencyType.USD));
        walletService.withdraw(charlie.getId(), new Money(new BigDecimal("50.00"), CurrencyType.USD));
        transactionService.transfer(charlie.getId(), alice.getId(), new Money(new BigDecimal("150.00"), CurrencyType.USD));

        // ASSERT - Total money should be conserved (minus the withdrawal)
        Either<WalletError, Money> aliceBalance = walletService.getBalance(alice.getId());
        Either<WalletError, Money> bobBalance = walletService.getBalance(bob.getId());
        Either<WalletError, Money> charlieBalance = walletService.getBalance(charlie.getId());

        BigDecimal totalAfter = aliceBalance.get().getAmount()
                .add(bobBalance.get().getAmount())
                .add(charlieBalance.get().getAmount());

        assertEquals(
                0,
                totalAfter.compareTo(new BigDecimal("2950.00")),
                "Total money should be 2950 (3000 - 50 withdrawn)"
        );
    }

}