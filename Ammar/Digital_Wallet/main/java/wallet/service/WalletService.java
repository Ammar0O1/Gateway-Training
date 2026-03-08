package wallet.service;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wallet.domain.*;
import wallet.error.InvalidAmountError;
import wallet.error.WalletError;
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

    public Either<WalletError, Transaction> deposit(UUID id, Money amount) {
        synchronized (getWalletLock(id)) {
            logger.info("Validating deposit amount: {}", amount);

            return Validators.validateAmount(amount)
                    .<WalletError>mapLeft(err -> err)  // Widen to WalletError
                    .flatMap(validAmount ->
                            Validators.validateWalletExists(walletRepository.findById(id), id)
                                    .<WalletError>mapLeft(err -> err)  // Widen to WalletError
                                    .flatMap(wallet ->
                                            Validators.validateSameCurrency(wallet.getBalance(), validAmount)
                                                    .<WalletError>mapLeft(err -> err)  // Widen to WalletError
                                                    .map(currencies -> wallet)
                                                    .flatMap(w ->
                                                            w.deposit(validAmount)
                                                                    .mapLeft(error -> (WalletError) new InvalidAmountError(validAmount))
                                                    )
                                    )
                                    .map(updatedWallet -> {
                                        walletRepository.save(updatedWallet);

                                        Transaction transaction = new Transaction(
                                                UUID.randomUUID(),
                                                Option.none(),
                                                Option.of(id),
                                                TransactionType.DEPOSIT,
                                                amount,
                                                LocalDateTime.now(),
                                                TransactionStatus.SUCCESS
                                        );

                                        transactionRepository.save(transaction);
                                        logger.info("Deposit successful for wallet id: {}, amount: {}", id, amount);

                                        return transaction;
                                    })
                    );
        }
    }

    public Either<WalletError, Transaction> withdraw(UUID id, Money amount) {
        synchronized (getWalletLock(id)) {
            logger.info("Validating withdrawal amount: {}", amount);

            return Validators.validateAmount(amount)
                    .<WalletError>mapLeft(err -> err)  // Widen to WalletError
                    .flatMap(validAmount ->
                            Validators.validateWalletExists(walletRepository.findById(id), id)
                                    .<WalletError>mapLeft(err -> err)  // Widen to WalletError
                                    .flatMap(wallet ->
                                            Validators.validateSameCurrency(wallet.getBalance(), validAmount)
                                                    .<WalletError>mapLeft(err -> err)  // Widen to WalletError
                                                    .map(currencies -> wallet)
                                                    .flatMap(w ->
                                                            w.withdraw(validAmount)
                                                                    .mapLeft(error -> (WalletError) new InvalidAmountError(validAmount))
                                                    )
                                    )
                                    .map(updatedWallet -> {
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

                                        return transaction;
                                    })
                    );
        }
    }

    public Either<WalletError, Money> getBalance(UUID id) {
        logger.info("Retrieving balance for wallet id: {}", id);

        return Validators.validateWalletExists(walletRepository.findById(id), id)
                .<WalletError>mapLeft(err -> err)  // Widen to WalletError
                .map(Wallet::getBalance);
    }

    public Either<WalletError, Wallet> getWallet(UUID id) {
        logger.info("Retrieving wallet for id: {}", id);

        return Validators.validateWalletExists(walletRepository.findById(id), id)
                .<WalletError>mapLeft(err -> err);  // Widen to WalletError
    }
}
