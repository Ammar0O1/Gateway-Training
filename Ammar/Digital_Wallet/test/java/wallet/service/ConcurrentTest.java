package wallet.service;

import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wallet.domain.*;
import wallet.repository.*;
import java.util.UUID;
import wallet.error.WalletError;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentTest {

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
    void testConcurrentTransfers() throws InterruptedException, ExecutionException {
        // ARRANGE - Create wallets with money
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Wallet bob = walletService.createWallet("Bob", CurrencyType.USD);
        Wallet charlie = walletService.createWallet("Charlie", CurrencyType.USD);

        Money deposit1000 = new Money(new BigDecimal("1000.00"), CurrencyType.USD);
        walletService.deposit(alice.getId(), deposit1000);
        walletService.deposit(bob.getId(), deposit1000);
        walletService.deposit(charlie.getId(), deposit1000);

        Money transfer10 = new Money(new BigDecimal("10.00"), CurrencyType.USD);

        // ACT - 10 threads each do 10 transfers simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Future<?> future = executor.submit(() -> {
                for (int j = 0; j < 10; j++) {
                    transactionService.transfer(alice.getId(), bob.getId(), transfer10);
                }
            });
            futures.add(future);
        }

        // Wait for all threads to complete
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // ASSERT - Check total money is conserved
        BigDecimal aliceBalance = walletService.getBalance(alice.getId()).get().getAmount();
        BigDecimal bobBalance = walletService.getBalance(bob.getId()).get().getAmount();
        BigDecimal charlieBalance = walletService.getBalance(charlie.getId()).get().getAmount();

        BigDecimal totalMoney = aliceBalance.add(bobBalance).add(charlieBalance);

        // Total should still be 3000 (money is conserved)
        assertEquals(new BigDecimal("3000.00"), totalMoney, "Total money should be conserved");

        // Alice should have less money
        assertTrue(aliceBalance.compareTo(new BigDecimal("1000.00")) < 0,
                "Alice should have transferred money out");

        // Bob should have more money
        assertTrue(bobBalance.compareTo(new BigDecimal("1000.00")) > 0,
                "Bob should have received money");
    }
    @Test
    void testConcurrentDeposits() throws InterruptedException, ExecutionException {
        // ARRANGE
        Wallet wallet = walletService.createWallet("TestUser", CurrencyType.USD);
        Money deposit10 = new Money(new BigDecimal("10.00"), CurrencyType.USD);

        // ACT - 10 threads each do 10 deposits = 100 deposits total
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Future<?> future = executor.submit(() -> {
                for (int j = 0; j < 10; j++) {
                    walletService.deposit(wallet.getId(), deposit10);
                }
            });
            futures.add(future);
        }

        // Wait for completion
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // ASSERT
        Either<WalletError, Money> balance = walletService.getBalance(wallet.getId());
        assertEquals(
                0,
                balance.get().getAmount().compareTo(new BigDecimal("1000.00")),
                "100 deposits of $10 should equal $1000"
        );
    }

    @Test
    void testConcurrentWithdrawals() throws InterruptedException, ExecutionException {
        // ARRANGE
        Wallet wallet = walletService.createWallet("TestUser", CurrencyType.USD);
        walletService.deposit(wallet.getId(), new Money(new BigDecimal("1000.00"), CurrencyType.USD));

        Money withdraw10 = new Money(new BigDecimal("10.00"), CurrencyType.USD);

        // ACT - 10 threads each do 10 withdrawals = 100 withdrawals
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Future<?> future = executor.submit(() -> {
                for (int j = 0; j < 10; j++) {
                    walletService.withdraw(wallet.getId(), withdraw10);
                }
            });
            futures.add(future);
        }

        // Wait for completion
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // ASSERT
        Either<WalletError, Money> balance = walletService.getBalance(wallet.getId());
        assertEquals(
                0,
                balance.get().getAmount().compareTo(BigDecimal.ZERO),
                "100 withdrawals of $10 should leave $0"
        );
    }

    @Test
    void testConcurrentMixedOperations() throws InterruptedException, ExecutionException {
        // ARRANGE
        Wallet wallet1 = walletService.createWallet("User1", CurrencyType.USD);
        Wallet wallet2 = walletService.createWallet("User2", CurrencyType.USD);

        walletService.deposit(wallet1.getId(), new Money(new BigDecimal("500.00"), CurrencyType.USD));
        walletService.deposit(wallet2.getId(), new Money(new BigDecimal("500.00"), CurrencyType.USD));

        Money amount10 = new Money(new BigDecimal("10.00"), CurrencyType.USD);

        // ACT - Multiple threads doing different operations
        ExecutorService executor = Executors.newFixedThreadPool(12);
        List<Future<?>> futures = new ArrayList<>();

        // 4 threads depositing to wallet1
        for (int i = 0; i < 4; i++) {
            futures.add(executor.submit(() -> {
                for (int j = 0; j < 5; j++) {
                    walletService.deposit(wallet1.getId(), amount10);
                }
            }));
        }

        // 4 threads withdrawing from wallet2
        for (int i = 0; i < 4; i++) {
            futures.add(executor.submit(() -> {
                for (int j = 0; j < 5; j++) {
                    walletService.withdraw(wallet2.getId(), amount10);
                }
            }));
        }

        // 4 threads transferring between wallets
        for (int i = 0; i < 4; i++) {
            futures.add(executor.submit(() -> {
                for (int j = 0; j < 5; j++) {
                    transactionService.transfer(wallet1.getId(), wallet2.getId(), amount10);
                }
            }));
        }

        // Wait for completion
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // ASSERT - Verify final state is consistent
        Either<WalletError, Money> balance1 = walletService.getBalance(wallet1.getId());
        Either<WalletError, Money> balance2 = walletService.getBalance(wallet2.getId());

        BigDecimal total = balance1.get().getAmount().add(balance2.get().getAmount());

        // Started with 1000 total, added 200 via deposits, removed 200 via withdrawals = 1000
        assertEquals(
                0,
                total.compareTo(new BigDecimal("1000.00")),
                "Total money should be conserved across all operations"
        );
    }

    @Test
    void testConcurrentTransfersNoMoneyLost() throws InterruptedException, ExecutionException {
        // ARRANGE - The ultimate banking test!
        Wallet alice = walletService.createWallet("Alice", CurrencyType.USD);
        Wallet bob = walletService.createWallet("Bob", CurrencyType.USD);
        Wallet charlie = walletService.createWallet("Charlie", CurrencyType.USD);
        Wallet diana = walletService.createWallet("Diana", CurrencyType.USD);

        // Each wallet starts with $250
        Money initial = new Money(new BigDecimal("250.00"), CurrencyType.USD);
        walletService.deposit(alice.getId(), initial);
        walletService.deposit(bob.getId(), initial);
        walletService.deposit(charlie.getId(), initial);
        walletService.deposit(diana.getId(), initial);

        BigDecimal totalBefore = new BigDecimal("1000.00");
        Money transfer5 = new Money(new BigDecimal("5.00"), CurrencyType.USD);

        // ACT - Create chaos with multiple concurrent transfers
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<Future<?>> futures = new ArrayList<>();

        // Random transfers between all wallets
        List<UUID> wallets = List.of(alice.getId(), bob.getId(), charlie.getId(), diana.getId());

        for (int i = 0; i < 20; i++) {
            final int threadNum = i;
            futures.add(executor.submit(() -> {
                for (int j = 0; j < 10; j++) {
                    int fromIdx = (threadNum + j) % 4;
                    int toIdx = (threadNum + j + 1) % 4;
                    transactionService.transfer(wallets.get(fromIdx), wallets.get(toIdx), transfer5);
                }
            }));
        }

        // Wait for completion
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // ASSERT - NO MONEY SHOULD BE LOST OR CREATED!
        BigDecimal totalAfter = walletService.getBalance(alice.getId()).get().getAmount()
                .add(walletService.getBalance(bob.getId()).get().getAmount())
                .add(walletService.getBalance(charlie.getId()).get().getAmount())
                .add(walletService.getBalance(diana.getId()).get().getAmount());

        assertEquals(
                0,
                totalAfter.compareTo(totalBefore),
                "Total money must be exactly preserved - banking cardinal rule!"
        );
    }
}