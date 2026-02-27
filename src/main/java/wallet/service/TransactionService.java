package wallet.service;

import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wallet.domain.*;
import wallet.error.*;
import wallet.repository.TransactionRepository;
import wallet.repository.WalletRepository;
import wallet.validation.Validators;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionService {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    // Per-wallet locks for thread safety
    private final ConcurrentHashMap<UUID, Object> walletLocks = new ConcurrentHashMap<>();

    public TransactionService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    // Get or create a lock for a specific wallet
    private Object getWalletLock(UUID walletId) {
        return walletLocks.computeIfAbsent(walletId, k -> new Object());
    }

    public Either<WalletError, Transaction> transfer(UUID fromId, UUID toId, Money amount) {
        logger.info("Initiating transfer from wallet {} to wallet {} of amount {}", fromId, toId, amount);

        // CRITICAL: Lock wallets in consistent order to prevent deadlock
        // Always lock the wallet with the smaller UUID first
        UUID firstLock = fromId.compareTo(toId) < 0 ? fromId : toId;
        UUID secondLock = fromId.compareTo(toId) < 0 ? toId : fromId;

        synchronized (getWalletLock(firstLock)) {
            synchronized (getWalletLock(secondLock)) {

                // Validate amount
                Either<InvalidAmountError, Money> amountValidation = Validators.validateAmount(amount);
                if (amountValidation.isLeft()) {
                    logger.warn("Invalid transfer amount: {}", amount);
                    return Either.left(amountValidation.getLeft());
                }

                // Validate not same wallet
                Either<SelfTransferError, Tuple2<UUID, UUID>> sameWalletValidation =
                        Validators.validateDifferentWallets(fromId, toId);
                if (sameWalletValidation.isLeft()) {
                    logger.warn("Transfer failed: Cannot transfer to same wallet");
                    return Either.left(sameWalletValidation.getLeft());
                }

                // Find source wallet
                Option<Wallet> fromWalletOption = walletRepository.findById(fromId);
                Either<WalletNotFoundError, Wallet> fromWalletValidation = Validators.validateWalletExists(fromWalletOption, fromId);
                if (fromWalletValidation.isLeft()) {
                    logger.warn("Source wallet not found for id: {}", fromId);
                    return Either.left(fromWalletValidation.getLeft());
                }
                Wallet fromWallet = fromWalletValidation.get();

                // Find destination wallet
                Option<Wallet> toWalletOption = walletRepository.findById(toId);
                Either<WalletNotFoundError, Wallet> toWalletValidation = Validators.validateWalletExists(toWalletOption, toId);
                if (toWalletValidation.isLeft()) {
                    logger.warn("Destination wallet not found for id: {}", toId);
                    return Either.left(toWalletValidation.getLeft());
                }
                Wallet toWallet = toWalletValidation.get();

                // Validate same currency between wallets
                Either<CurrencyMismatchError, Tuple2<Money, Money>> currencyValidation =
                        Validators.validateSameCurrency(fromWallet.getBalance(), toWallet.getBalance());
                if (currencyValidation.isLeft()) {
                    logger.warn("Transfer failed: Currency mismatch between wallets");
                    return Either.left(currencyValidation.getLeft());
                }

                // Validate amount currency matches wallet currency
                Either<CurrencyMismatchError, Tuple2<Money, Money>> amountCurrencyValidation =
                        Validators.validateSameCurrency(fromWallet.getBalance(), amount);
                if (amountCurrencyValidation.isLeft()) {
                    logger.warn("Transfer failed: Amount currency doesn't match wallet currency");
                    return Either.left(amountCurrencyValidation.getLeft());
                }

                // Perform withdrawal from source wallet
                Either<String, Wallet> withdrawResult = fromWallet.withdraw(amount);
                if (withdrawResult.isLeft()) {
                    logger.warn("Transfer failed: {}", withdrawResult.getLeft());
                    return Either.left(new InvalidAmountError(amount));
                }
                Wallet updatedFromWallet = withdrawResult.get();

                // Perform deposit to destination wallet
                Either<String, Wallet> depositResult = toWallet.deposit(amount);
                if (depositResult.isLeft()) {
                    logger.warn("Transfer failed: {}", depositResult.getLeft());
                    return Either.left(new InvalidAmountError(amount));
                }
                Wallet updatedToWallet = depositResult.get();

                // Save both wallets
                walletRepository.save(updatedFromWallet);
                walletRepository.save(updatedToWallet);

                // Create transaction record
                Transaction transaction = new Transaction(
                        UUID.randomUUID(),
                        Option.of(fromId),
                        Option.of(toId),
                        TransactionType.TRANSFER,
                        amount,
                        LocalDateTime.now(),
                        TransactionStatus.SUCCESS
                );

                transactionRepository.save(transaction);

                logger.info("Transfer successful: {}", transaction.getId());
                return Either.right(transaction);
            }
        }
    }

    public List<Transaction> getHistory(UUID walletId) {
        return transactionRepository.findByWallet(walletId).stream()
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .toList();
    }

    public List<Transaction> getHistoryByType(UUID walletId, TransactionType type) {
        return transactionRepository.findByWallet(walletId).stream()
                .filter(tx -> tx.getType() == type)
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .toList();
    }

    public List<Transaction> getHistoryByStatus(UUID walletId, TransactionStatus status) {
        return transactionRepository.findByWallet(walletId).stream()
                .filter(tx -> tx.getStatus() == status)
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .toList();
    }
}