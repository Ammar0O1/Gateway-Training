package wallet.error;
import lombok.Value;
import java.util.UUID;
@Value
public class WalletNotFoundError  implements WalletError{
    UUID walletId;
    @Override
    public String getMessage() {
        return "Wallet not found: " + walletId ;
    }
}
