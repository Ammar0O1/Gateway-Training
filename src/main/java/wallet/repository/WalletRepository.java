package wallet.repository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wallet.domain.Wallet;

public class WalletRepository {

    private static final Logger logger =
            LoggerFactory.getLogger(WalletRepository.class);

    private final Map<UUID, Wallet> wallets =
            new ConcurrentHashMap<>();

    public Wallet save(Wallet wallet) {
        logger.info("Saving wallet: {}", wallet);
        wallets.put(wallet.getId(), wallet);
        return wallet;
    }

    public Option <Wallet> findById(UUID id) {
        logger.info("Finding wallet by id: {}", id);

        Option<Wallet> result = Option.of(wallets.get(id));

        result.onEmpty(() -> logger.warn("Wallet not found with id: {}", id))
                .peek(w -> logger.info("Wallet found: {}", id));
        return result;
    }

    public Option <Wallet> update(UUID id, Function<Wallet, Wallet> updater){
        logger.info("Updating wallet with id: {}", id);
        return findById(id)
                .map(oldWallet ->{
                    Wallet newWallet = updater.apply(oldWallet);
                    wallets.put(id, newWallet);
                    logger.info("Wallet updated successfully: {}", id);
                    return newWallet;
                });

    }

    public boolean delete(UUID id){
        logger.info("Deleting wallet with id: {}", id);

        Wallet removed = wallets.remove(id);
        if (removed != null) {
            logger.info("Wallet deleted successfully: {}", id);
            return true;
        } else {
            logger.warn("Wallet not found for deletion with id: {}", id);
            return false;
        }
    }

    public List <Wallet> findAll() {
        logger.debug("Finding all wallets");
        return List.copyOf(wallets.values());
    }




}
