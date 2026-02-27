package wallet.error;

import lombok.Value;
import wallet.domain.Money;

@Value
public class InvalidAmountError implements WalletError {
    Money amount;

    @Override
    public String getMessage() {
        return "Invalid amount: " + amount;
    }
}
