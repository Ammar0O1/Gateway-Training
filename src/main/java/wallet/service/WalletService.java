package wallet.service;

import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wallet.domain.*;
import wallet.error.CurrencyMismatchError;
import wallet.error.InvalidAmountError;
import wallet.error.WalletError;
import wallet.error.WalletNotFoundError;
import wallet.repository.TransactionRepository;
import wallet.repository.WalletRepository;
import wallet.validation.Validators;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WalletService {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final ConcurrentHashMap<UUID, Object> walletLocks = new ConcurrentHashMap<>();
    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }
    private Object getWalletLock(UUID walletId) {
        return walletLocks.computeIfAbsent(walletId, k -> new Object());
    }

    public Wallet createWallet(String ownerName, CurrencyType currency) {
        logger.info("Creating wallet for owner: {}", ownerName);

        Money initialBalance = new Money(BigDecimal.ZERO, currency);

        Wallet wallet = new Wallet(
                UUID.randomUUID(),
                ownerName,
                initialBalance,
                currency,
                LocalDateTime.now(),
                1L);

        walletRepository.save(wallet);

        logger.info("Wallet created with id: {}", wallet.getId());

        return wallet;
    }

    public  Either<WalletError, Transaction> deposit(UUID id, Money amount) {
        synchronized (getWalletLock(id)){
        logger.info("Validating deposit amount: {}", amount);

        // Validate amount
        Either<InvalidAmountError, Money> amountValidation = Validators.validateAmount(amount);
        if (amountValidation.isLeft()) {
            logger.warn("Invalid deposit amount: {}", amount);
            return Either.left(amountValidation.getLeft());
        }

        // Find wallet
        Option<Wallet> walletOption = walletRepository.findById(id);
        Either<WalletNotFoundError, Wallet> walletValidation = Validators.validateWalletExists(walletOption, id);
        if (walletValidation.isLeft()) {
            logger.warn("Wallet not found for id: {}", id);
            return Either.left(walletValidation.getLeft());
        }
        Wallet wallet = walletValidation.get();

        // Validate same currency
        Either<CurrencyMismatchError, Tuple2<Money, Money>> currencyValidation =
                Validators.validateSameCurrency(wallet.getBalance(), amount);
        if (currencyValidation.isLeft()) {
            logger.warn("Deposit failed: Currency mismatch");
            return Either.left(currencyValidation.getLeft());
        }

        // Perform deposit
        Either<String, Wallet> depositResult = wallet.deposit(amount);
        if (depositResult.isLeft()) {
            logger.warn("Deposit failed: {}", depositResult.getLeft());
            return Either.left(new InvalidAmountError(amount));
        }
        Wallet updatedWallet = depositResult.get();

        walletRepository.save(updatedWallet);

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                Option.none(),           // fromWalletId - external source
                Option.of(id),           // toWalletId - this wallet
                TransactionType.DEPOSIT,
                amount,
                LocalDateTime.now(),
                TransactionStatus.SUCCESS
        );

        transactionRepository.save(transaction);

        logger.info("Deposit successful for wallet id: {}, amount: {}", id, amount);
        return Either.right(transaction);
    }
    }

    public  Either<WalletError, Transaction> withdraw(UUID id, Money amount) {
        synchronized (getWalletLock(id)) {
            logger.info("Validating withdrawal amount: {}", amount);

            // Validate amount
            Either<InvalidAmountError, Money> amountValidation = Validators.validateAmount(amount);
            if (amountValidation.isLeft()) {
                logger.warn("Invalid withdrawal amount: {}", amount);
                return Either.left(amountValidation.getLeft());
            }

            // Find wallet
            Option<Wallet> walletOption = walletRepository.findById(id);
            Either<WalletNotFoundError, Wallet> walletValidation = Validators.validateWalletExists(walletOption, id);
            if (walletValidation.isLeft()) {
                logger.warn("Wallet not found for id: {}", id);
                return Either.left(walletValidation.getLeft());
            }
            Wallet wallet = walletValidation.get();

            // Validate same currency
            Either<CurrencyMismatchError, Tuple2<Money, Money>> currencyValidation =
                    Validators.validateSameCurrency(wallet.getBalance(), amount);
            if (currencyValidation.isLeft()) {
                logger.warn("Withdrawal failed: Currency mismatch");
                return Either.left(currencyValidation.getLeft());
            }

            // Perform withdrawal
            Either<String, Wallet> withdrawResult = wallet.withdraw(amount);
            if (withdrawResult.isLeft()) {
                logger.warn("Withdrawal failed: {}", withdrawResult.getLeft());
                return Either.left(new InvalidAmountError(amount));
            }
            Wallet updatedWallet = withdrawResult.get();

            walletRepository.save(updatedWallet);

            Transaction transaction = new Transaction(
                    UUID.randomUUID(),
                    Option.of(id),
                    Option.none(),
                    TransactionType.WITHDRAW,
                    amount,
                    LocalDateTime.now(),
                    TransactionStatus.SUCCESS
            );

            transactionRepository.save(transaction);

            logger.info("Withdrawal successful for wallet id: {}, amount: {}", id, amount);
            return Either.right(transaction);
        }
    }



    public Either<WalletError,Money> getBalance(UUID id) {
        logger.info("Retrieving balance for wallet id: {}", id);

        Option <Wallet> walletOption = walletRepository.findById(id);
        Either<WalletNotFoundError, Wallet> walletValidation = Validators.validateWalletExists(walletOption, id);
        if (walletValidation.isLeft()) {
            logger.warn("Wallet not found for id: {}", id);
            return Either.left(walletValidation.getLeft());
    }
        return Either.right(walletValidation.get().getBalance());
    }

    public Either<WalletError, Wallet> getWallet(UUID id) {
        logger.info("Retrieving wallet for id: {}", id);

        Option<Wallet> walletOption = walletRepository.findById(id);
        Either<WalletNotFoundError, Wallet> walletValidation = Validators.validateWalletExists(walletOption, id);
        if (walletValidation.isLeft()) {
            logger.warn("Wallet not found for id: {}", id);
            return Either.left(walletValidation.getLeft());
        }
        return Either.right(walletValidation.get());
    }


}