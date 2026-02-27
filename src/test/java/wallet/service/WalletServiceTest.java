package wallet.service;

import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wallet.domain.CurrencyType;
import wallet.domain.Money;
import wallet.domain.Transaction;
import wallet.domain.Wallet;
import wallet.error.WalletError;
import wallet.repository.TransactionRepository;
import wallet.repository.WalletRepository;
import wallet.validation.Validators;
import java.util.UUID;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WalletServiceTest {

    private WalletRepository walletRepository;
    private TransactionRepository transactionRepository;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletRepository = new WalletRepository();
        transactionRepository = new TransactionRepository();
        walletService = new WalletService(walletRepository, transactionRepository);
    }

    @Test
    void testCreateWallet() {
        // ARRANGE
        String ownerName = "Alice";
        CurrencyType currency = CurrencyType.USD;

        // ACT
        Wallet wallet = walletService.createWallet(ownerName, currency);

        // ASSERT
        assertNotNull(wallet);
        assertEquals(ownerName, wallet.getOwnerName());
        assertEquals(currency, wallet.getCurrency());
    }

    @Test
    void testSuccessfulDeposit() {
        String ownerName = "Bob";
        CurrencyType currency = CurrencyType.USD;
        Wallet wallet = walletService.createWallet(ownerName, currency);

        // ACT
        Money depositAmount = new Money(new BigDecimal("100.00"), currency);

        Either<WalletError, Transaction> result = walletService.deposit(wallet.getId(), depositAmount);

        assertTrue(result.isRight(), "Deposit succeeded ");

        Either<WalletError, Money> balance = walletService.getBalance(wallet.getId());
        assertTrue(balance.isRight(), "Balance retrieval succeeded");
        assertEquals(new BigDecimal("100.00"),
                balance.get().getAmount(),
                "Balance should be 100.00"
        );

    }

    @Test
    void testDepositZeroAmount() {
        // ARRANGE
        Wallet wallet = walletService.createWallet("Charlie", CurrencyType.USD);
        Money zeroAmount = new Money(BigDecimal.ZERO, CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                walletService.deposit(wallet.getId(), zeroAmount);

        // ASSERT
        assertTrue(result.isLeft(), "Deposit with zero amount should fail");
    }

    @Test
    void testWithdrawInsufficientBalance() {
        // ARRANGE
        Wallet wallet = walletService.createWallet("Eve", CurrencyType.USD);
        Money depositAmount = new Money(new BigDecimal("50.00"), CurrencyType.USD);
        walletService.deposit(wallet.getId(), depositAmount);

        Money withdrawAmount = new Money(new BigDecimal("100.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                walletService.withdraw(wallet.getId(), withdrawAmount);

        // ASSERT
        assertTrue(result.isLeft(), "Withdraw should fail - insufficient balance");
    }

    @Test
    void testSuccessfulWithdraw() {
        // ARRANGE - Create wallet and deposit money first
        Wallet wallet = walletService.createWallet("David", CurrencyType.USD);
        Money depositAmount = new Money(new BigDecimal("100.00"), CurrencyType.USD);
        walletService.deposit(wallet.getId(), depositAmount);

        Money withdrawAmount = new Money(new BigDecimal("30.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                walletService.withdraw(wallet.getId(), withdrawAmount);

        // ASSERT
        assertTrue(result.isRight(), "Withdraw should succeed");

        Either<WalletError, Money> balance = walletService.getBalance(wallet.getId());
        assertEquals(
                new BigDecimal("70.00"),
                balance.get().getAmount(),
                "Balance should be 70.00 after withdrawal"
        );
    }
    @Test
    void testDepositToNonExistentWallet() {
        // ARRANGE
        UUID nonExistentId = UUID.randomUUID();
        Money amount = new Money(new BigDecimal("100.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                walletService.deposit(nonExistentId, amount);

        // ASSERT
        assertTrue(result.isLeft(), "Deposit to non-existent wallet should fail");
    }

    @Test
    void testWithdrawFromNonExistentWallet() {
        // ARRANGE
        UUID nonExistentId = UUID.randomUUID();
        Money amount = new Money(new BigDecimal("50.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                walletService.withdraw(nonExistentId, amount);

        // ASSERT
        assertTrue(result.isLeft(), "Withdraw from non-existent wallet should fail");
    }

    @Test
    void testMultipleDepositsAccumulate() {
        // ARRANGE
        Wallet wallet = walletService.createWallet("TestUser", CurrencyType.USD);
        Money deposit1 = new Money(new BigDecimal("100.00"), CurrencyType.USD);
        Money deposit2 = new Money(new BigDecimal("50.00"), CurrencyType.USD);
        Money deposit3 = new Money(new BigDecimal("25.00"), CurrencyType.USD);

        // ACT
        walletService.deposit(wallet.getId(), deposit1);
        walletService.deposit(wallet.getId(), deposit2);
        walletService.deposit(wallet.getId(), deposit3);

        // ASSERT
        Either<WalletError, Money> balance = walletService.getBalance(wallet.getId());
        assertTrue(balance.isRight());
        assertEquals(
                new BigDecimal("175.00"),
                balance.get().getAmount(),
                "Multiple deposits should accumulate"
        );
    }

    @Test
    void testWithdrawExactBalance() {
        // ARRANGE
        Wallet wallet = walletService.createWallet("TestUser", CurrencyType.USD);
        Money deposit = new Money(new BigDecimal("100.00"), CurrencyType.USD);
        walletService.deposit(wallet.getId(), deposit);

        Money withdrawAll = new Money(new BigDecimal("100.00"), CurrencyType.USD);

        // ACT
        Either<WalletError, Transaction> result =
                walletService.withdraw(wallet.getId(), withdrawAll);

        // ASSERT
        assertTrue(result.isRight(), "Should be able to withdraw exact balance");

        Either<WalletError, Money> balance = walletService.getBalance(wallet.getId());
        assertEquals(
                0,
                balance.get().getAmount().compareTo(BigDecimal.ZERO),
                "Balance should be exactly zero"
        );
    }

    @Test
    void testDepositCurrencyMismatch() {
        // ARRANGE
        Wallet wallet = walletService.createWallet("TestUser", CurrencyType.USD);
        Money euroAmount = new Money(new BigDecimal("100.00"), CurrencyType.EUR);

        // ACT
        Either<WalletError, Transaction> result =
                walletService.deposit(wallet.getId(), euroAmount);

        // ASSERT
        assertTrue(result.isLeft(), "Deposit with wrong currency should fail");
    }
}