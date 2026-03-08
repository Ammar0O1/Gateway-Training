package wallet.service;

import io.vavr.Tuple;
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

    private final ConcurrentHashMap<UUID, Object> walletLocks = new ConcurrentHashMap<>();

    public TransactionService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    private Object getWalletLock(UUID walletId) {
        return walletLocks.computeIfAbsent(walletId, k -> new Object());
    }

    public Either<WalletError, Transaction> transfer(UUID fromId, UUID toId, Money amount) {
        logger.info("Initiating transfer from wallet {} to wallet {} of amount {}", fromId, toId, amount);

        // Lock wallets in consistent order to prevent deadlock
        UUID firstLock = fromId.compareTo(toId) < 0 ? fromId : toId;
        UUID secondLock = fromId.compareTo(toId) < 0 ? toId : fromId;

        synchronized (getWalletLock(firstLock)) {
            synchronized (getWalletLock(secondLock)) {

                return Validators.validateAmount(amount)
                        .<WalletError>mapLeft(err -> err)
                        .flatMap(validAmount ->
                                Validators.validateDifferentWallets(fromId, toId)
                                        .<WalletError>mapLeft(err -> err)
                                        .flatMap(walletIds ->
                                                // Get both wallets
                                                getBothWallets(fromId, toId)
                                                        .flatMap(wallets -> {
                                                            Wallet fromWallet = wallets._1;
                                                            Wallet toWallet = wallets._2;

                                                            // Validate currencies
                                                            return Validators.validateSameCurrency(fromWallet.getBalance(), toWallet.getBalance())
                                                                    .<WalletError>mapLeft(err -> err)
                                                                    .flatMap(currencies ->
                                                                            Validators.validateSameCurrency(fromWallet.getBalance(), validAmount)
                                                                                    .<WalletError>mapLeft(err -> err)
                                                                                    .map(c -> Tuple.of(fromWallet, toWallet))
                                                                    );
                                                        })
                                                        .flatMap(wallets -> {
                                                            Wallet fromWallet = wallets._1;
                                                            Wallet toWallet = wallets._2;

                                                            // Perform transfer
                                                            return performTransfer(fromWallet, toWallet, validAmount, fromId, toId);
                                                        })
                                        )
                        );
            }
        }
    }


    private Either<WalletError, Tuple2<Wallet, Wallet>> getBothWallets(UUID fromId, UUID toId) {
        return Validators.validateWalletExists(walletRepository.findById(fromId), fromId)
                .<WalletError>mapLeft(err -> err)
                .flatMap(fromWallet ->
                        Validators.validateWalletExists(walletRepository.findById(toId), toId)
                                .<WalletError>mapLeft(err -> err)
                                .map(toWallet -> Tuple.of(fromWallet, toWallet))
                );
    }

    // Helper: Perform the actual transfer
    private Either<WalletError, Transaction> performTransfer(
            Wallet fromWallet,
            Wallet toWallet,
            Money amount,
            UUID fromId,
            UUID toId) {

        return fromWallet.withdraw(amount)
                .mapLeft(error -> (WalletError) new InvalidAmountError(amount))
                .flatMap(updatedFromWallet ->
                        toWallet.deposit(amount)
                                .mapLeft(error -> (WalletError) new InvalidAmountError(amount))
                                .map(updatedToWallet -> Tuple.of(updatedFromWallet, updatedToWallet))
                )
                .map(updatedWallets -> {
                    // Save both wallets
                    walletRepository.save(updatedWallets._1);
                    walletRepository.save(updatedWallets._2);

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

                    return transaction;
                });
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
