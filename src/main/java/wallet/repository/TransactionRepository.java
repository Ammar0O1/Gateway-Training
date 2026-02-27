package wallet.repository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wallet.domain.Transaction;


public class TransactionRepository{
private final List<Transaction> transactions = new CopyOnWriteArrayList<>();

//logger
private static final Logger logger =
        LoggerFactory.getLogger(TransactionRepository.class);

public Transaction save(Transaction transaction) {
    logger.info("Saving transaction: {}", transaction);
    transactions.add(transaction);
    return transaction;

}

public Option<Transaction> findById(UUID Id) {
    logger.info("Finding transactions by  id: {}", Id);
     return Option.ofOptional(transactions.stream()
            .filter(t -> t.getId().equals(Id))
            .findFirst()
    );
}

public List<Transaction> findAll() {
logger.debug("Finding all transactions");
    return List.copyOf(transactions);
}

    public List<Transaction> findByWallet(UUID walletId) {
        logger.info("Finding transactions by wallet id: {}", walletId);

        return transactions.stream()
                .filter(t -> t.involvesWallet(walletId))
                .toList();}
}
