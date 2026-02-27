package wallet.error;
import lombok.Value;
import java.util.UUID;
@Value
public class ConcurrentModificationError implements WalletError {
    UUID walletId;
    long expectedVersion;
    long actualVersion;


    @Override
    public String getMessage() {
        return "Concurrent modification on wallet: " + walletId +
                " expected version: " + expectedVersion +
                " but found version: " + actualVersion;
    }
}
