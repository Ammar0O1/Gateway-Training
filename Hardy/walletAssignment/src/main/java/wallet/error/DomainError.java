package wallet.error;

public enum DomainError {

    WALLET_NOT_FOUND("Wallet not found"),
    INVALID_AMOUNT("Invalid amount"),
    INSUFFICIENT_BALANCE("Insufficient balance"),
    SELF_TRANSFER("Cannot transfer to the same wallet"),
    CURRENCY_MISMATCH("Cannot transfer between different currencies");

    private final String message;

    DomainError(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}