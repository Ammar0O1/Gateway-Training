package wallet.service;

import io.vavr.control.Either;
import io.vavr.control.Option;
import wallet.model.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import wallet.error.DomainError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wallet.model.Currency;

public class WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final Map<UUID, Wallet> wallets = new ConcurrentHashMap<>();
    private final List<Transaction> transactions = Collections.synchronizedList(new ArrayList<>());

    public Wallet createWallet(String ownerName, Currency currency) {
        Wallet wallet = new Wallet(ownerName, currency);
        wallets.put(wallet.getId(), wallet);
        return wallet;
    }

    private Option<Wallet> findWallet(UUID id) {
        return Option.of(wallets.get(id));
    }

    public Either<DomainError, Wallet> deposit(UUID id, BigDecimal amount) {
        return findWallet(id)
                .toEither(DomainError.WALLET_NOT_FOUND)
                .flatMap(wallet ->
                        wallet.deposit(amount)
                                .peek(w -> {
                                    logger.info("Deposit SUCCESS | walletId={} | amount={}", id, amount);
                                    transactions.add(new Transaction(TransactionType.DEPOSIT, amount, TransactionStatus.SUCCESS, null, wallet.getId()));
                                })
                                .peekLeft(e -> {
                                    logger.warn("Deposit FAILED | walletId={} | amount={} | error={}", id, amount, e);
                                    transactions.add(new Transaction(TransactionType.DEPOSIT, amount, TransactionStatus.FAILED, null, wallet.getId()));
                                })
                );
    }

    public Either<DomainError, Wallet> withdraw(UUID id, BigDecimal amount) {
        return findWallet(id)
                .toEither(DomainError.WALLET_NOT_FOUND)
                .flatMap(wallet ->
                        wallet.withdraw(amount)
                                .peek(w -> {
                                    logger.info("Withdraw SUCCESS | walletId={} | amount={}", id, amount);
                                    transactions.add(new Transaction(TransactionType.WITHDRAWAL, amount, TransactionStatus.SUCCESS, wallet.getId(), null));
                                })
                                .peekLeft(e -> {
                                    logger.warn("Withdraw FAILED | walletId={} | amount={} | error={}", id, amount, e);
                                    transactions.add(new Transaction(TransactionType.WITHDRAWAL, amount, TransactionStatus.FAILED, wallet.getId(), null));
                                })
                );
    }

    public synchronized Either<DomainError, Transaction> transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        if (fromWalletId.equals(toWalletId))
            return Either.left(DomainError.SELF_TRANSFER);
        else{
        return findWallet(fromWalletId)
                .toEither(DomainError.WALLET_NOT_FOUND)
                .flatMap(from ->
                        findWallet(toWalletId)
                                .toEither(DomainError.WALLET_NOT_FOUND)
                                .flatMap(to -> {
                                    if (!from.getCurrency().equals(to.getCurrency()))
                                        return Either.left(DomainError.CURRENCY_MISMATCH);
                                    return from.withdraw(amount)
                                            .flatMap(w -> to.deposit(amount)
                                                    .map(x -> {
                                                        logger.info("Transfer SUCCESS | fromWalletId={} | toWalletId={} | amount={}", fromWalletId, toWalletId, amount);
                                                        Transaction t = new Transaction(TransactionType.TRANSFER, amount, TransactionStatus.SUCCESS, fromWalletId, toWalletId);
                                                        transactions.add(t);
                                                        return t;
                                                    }))
                                            .peekLeft(e -> {
                                                logger.warn("Transfer FAILED | fromWalletId={} | toWalletId={} | amount={} | error={}", fromWalletId, toWalletId, amount, e);
                                                transactions.add(new Transaction(TransactionType.TRANSFER, amount, TransactionStatus.FAILED, fromWalletId, toWalletId));
                                            });
                                }));
    }}
    public Either<DomainError, BigDecimal> getBalance(UUID id) {
        return findWallet(id)
                .toEither(DomainError.WALLET_NOT_FOUND)
                .map(Wallet::getBalance);
    }

    public Either<DomainError, List<Transaction>> getTransactionHistory(UUID walletId) {
        return findWallet(walletId)
                .toEither(DomainError.WALLET_NOT_FOUND)
                .map(w -> transactions.stream()
                        .filter(t ->
                                walletId.equals(t.getFromWallet()) ||
                                        walletId.equals(t.getToWallet()))
                        .collect(Collectors.toList()));
    }
}