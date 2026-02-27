package wallet.error;

import lombok.Value;
import wallet.domain.CurrencyType;

@Value
public class CurrencyMismatchError implements WalletError {
    CurrencyType currency1;
    CurrencyType currency2;
    @Override
    public String getMessage() {
        return "Currency mismatch: " + currency1 + " vs " + currency2;
    }
}
