package wallet.validation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import wallet.domain.Money;
import wallet.domain.Wallet;
import wallet.error.*;

import java.util.Objects;
import java.util.UUID;

public class Validators {
    private Validators() {}

    public static Either <InvalidAmountError, Money> validateAmount(Money amount){
        if (amount == null) {
            return Either.left(new InvalidAmountError(null));
        }
        if (!amount.isPositive()){
            return Either.left(new InvalidAmountError(amount));
        }
        return Either.right(amount);

    }

    public static Either<InsufficientBalanceError, Wallet>validateSufficientBalance(Wallet wallet, Money money){
        Either<String, Boolean> result = wallet.hasBalance(money);
        if (result.isLeft() || !result.get()) {
            return Either.left(new InsufficientBalanceError(
                    wallet.getId(),
                    money,
            wallet.getBalance()
            ));
        }
        return Either.right(wallet);
    }

    public static Either<SelfTransferError, Tuple2<UUID, UUID>> validateNotSelfTransfer(UUID fromWalletId, UUID toWalletId) {
        if(fromWalletId != null && fromWalletId.equals(toWalletId)) {
            return Either.left(new SelfTransferError(fromWalletId));
        }
        return Either.right(Tuple.of(fromWalletId, toWalletId));
}
    public static Either<CurrencyMismatchError, Tuple2<Money, Money>> validateSameCurrency(Money money1, Money money2) {
        if (money1 != null && money2 != null && money1.getCurrency() != money2.getCurrency()) {
            return Either.left(new CurrencyMismatchError(money1.getCurrency(), money2.getCurrency()));
        }
        return Either.right(Tuple.of(money1, money2));
    }

    public static Either<WalletNotFoundError, Wallet> validateWalletExists(Option<Wallet> walletOption, UUID walletId) {
        if (walletOption.isEmpty()) {
            return Either.left(new WalletNotFoundError(walletId));
        }
        return Either.right(walletOption.get());
    }
    public static Either<ConcurrentModificationError, Wallet> validateVersion (Wallet wallet, long expectedVersion) {
    if (!Objects.equals(wallet.getVersion(), expectedVersion)) {
        return Either.left(new ConcurrentModificationError(wallet.getId(), expectedVersion, wallet.getVersion()));
    }
    return Either.right(wallet);
    }
    public static Either<SelfTransferError, Tuple2<UUID, UUID>> validateDifferentWallets(UUID fromWalletId, UUID toWalletId) {
        if (fromWalletId.equals(toWalletId)) {
            return Either.left(new SelfTransferError(fromWalletId));
        }
        return Either.right(new Tuple2<>(fromWalletId, toWalletId));
    }

}
