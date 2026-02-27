package wallet.domain;
import io.vavr.control.Option;
import lombok.Value;
import java.util.UUID;
import java.time.LocalDateTime;


@Value
public class Transaction {
    UUID id;
    Option<UUID> fromWalletId;
    Option<UUID> toWalletId;
    TransactionType type;
    Money amount;
    LocalDateTime timestamp;
    TransactionStatus status;

    // isInvolvingWallet

    public boolean involvesWallet (UUID walletId) {
        if (walletId == null) {
            return false;
        }
        return fromWalletId.contains(walletId) || toWalletId.contains(walletId);
    }

    public boolean isSuccess() {
        return this.status == TransactionStatus.SUCCESS;
    }

}
