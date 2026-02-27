package wallet.error;
import java.util.UUID;
import lombok.Value;

@Value
public class SelfTransferError implements WalletError {
    UUID walletId;
    @Override
    public String getMessage() {

        return "Cannot transfer to the same wallet: " + walletId;
    }
}
