package wallet.error;
import lombok.Value;
import wallet.domain.Money;
import java.util.UUID;

@Value
public class InsufficientBalanceError implements WalletError {
    UUID walletId;
    Money required;
    Money available;
    @Override

    public String getMessage() {
        return "Insufficient balance in wallet: " + walletId +
                ". Required: " + required +
                ", Available: " + available;
    }
}
