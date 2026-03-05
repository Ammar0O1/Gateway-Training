package wallet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wallet.model.Currency;
import wallet.model.Wallet;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WalletServiceTest {

    private WalletService walletService;

    @BeforeEach
    void setup() {
        walletService = new WalletService();
    }

    @Test
    void shouldTransferSuccessfully() {
        Wallet w1 = walletService.createWallet("Alice", Currency.USD);
        Wallet w2 = walletService.createWallet("Bob", Currency.USD);

        walletService.deposit(w1.getId(), new BigDecimal("100"));

        var result = walletService.transfer(
                w1.getId(),
                w2.getId(),
                new BigDecimal("50")
        );

        assertTrue(result.isRight());
        assertEquals(new BigDecimal("50"),
                walletService.getBalance(w1.getId()).get());
        assertEquals(new BigDecimal("50"),
                walletService.getBalance(w2.getId()).get());
    }
    @Test
    void shouldFailWhenInsufficientBalance() {
        Wallet w1 = walletService.createWallet("Alice", Currency.USD);
        Wallet w2 = walletService.createWallet("Bob", Currency.USD);

        walletService.deposit(w1.getId(), new BigDecimal("30"));

        var result = walletService.transfer(
                w1.getId(),
                w2.getId(),
                new BigDecimal("100")
        );

        assertTrue(result.isLeft());
    }

    @Test
    void shouldFailOnSelfTransfer() {
        Wallet w1 = walletService.createWallet("Alice", Currency.USD);

        var result = walletService.transfer(
                w1.getId(),
                w1.getId(),
                new BigDecimal("10")
        );
        assertTrue(result.isLeft());
    }
    @Test
    void shouldHandleConcurrentTransfersSafely() throws InterruptedException {

        Wallet w1 = walletService.createWallet("Alice", Currency.USD);
        Wallet w2 = walletService.createWallet("Bob", Currency.USD);

        walletService.deposit(w1.getId(), new BigDecimal("1000"));

        Runnable task = () ->
                walletService.transfer(
                        w1.getId(),
                        w2.getId(),
                        new BigDecimal("100")
                );

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        Thread t3 = new Thread(task);
        Thread t4 = new Thread(task);
        Thread t5 = new Thread(task);

        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();

        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();

        assertEquals(new BigDecimal("500"),
                walletService.getBalance(w1.getId()).get());
        assertEquals(new BigDecimal("500"),
                walletService.getBalance(w2.getId()).get());
    }
}