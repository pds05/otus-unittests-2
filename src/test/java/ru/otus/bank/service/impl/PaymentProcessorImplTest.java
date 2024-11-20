package ru.otus.bank.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.bank.entity.Account;
import ru.otus.bank.entity.Agreement;
import ru.otus.bank.service.AccountService;
import ru.otus.bank.service.exception.AccountException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentProcessorImplTest {
    Agreement sourceAgreement;
    Agreement destinationAgreement;
    Account sourceAccount;
    Account destinationAccount;

    @Mock
    AccountService accountService;

    @InjectMocks
    PaymentProcessorImpl paymentProcessor;

    @BeforeEach
    public void init() {
        sourceAgreement = new Agreement();
        sourceAgreement.setId(1L);

        destinationAgreement = new Agreement();
        destinationAgreement.setId(2L);

        sourceAccount = new Account();
        sourceAccount.setAmount(new BigDecimal(100));
        sourceAccount.setType(0);
        sourceAccount.setId(10L);

        destinationAccount = new Account();
        destinationAccount.setAmount(BigDecimal.ZERO);
        destinationAccount.setType(0);
        destinationAccount.setId(20L);
    }

    @Test
    public void testTransfer() {
        when(accountService.getAccounts(argThat(argument -> argument != null && argument.getId() == 1L))).thenReturn(List.of(sourceAccount));
        when(accountService.getAccounts(argThat(argument -> argument != null && argument.getId() == 2L))).thenReturn(List.of(destinationAccount));
        when(accountService.makeTransfer(10L, 20L, BigDecimal.ONE)).thenReturn(true);

        boolean result = paymentProcessor.makeTransfer(sourceAgreement, destinationAgreement,
                0, 0, BigDecimal.ONE);

        assertTrue(result);
    }

    @Test
    public void testTransferNotFound() {
        when(accountService.getAccounts(argThat(argument -> argument != null && argument.getId() == 1L))).thenReturn(List.of(sourceAccount));
        when(accountService.getAccounts(argThat(argument -> argument != null && argument.getId() == 2L))).thenReturn(new ArrayList<>());

        AccountException result = assertThrows(AccountException.class, () -> paymentProcessor.makeTransfer(sourceAgreement, destinationAgreement,
                0, 0, BigDecimal.ONE));

        assertEquals("Account not found", result.getMessage());

        result = assertThrows(AccountException.class, () -> paymentProcessor.makeTransfer(destinationAgreement, sourceAgreement,
                0, 0, BigDecimal.ONE));

        assertEquals("Account not found", result.getMessage());

        verify(accountService, times(3)).getAccounts(any());
    }

    @Test
    public void testMakeTransferWithComission() {
        ArgumentCaptor<BigDecimal> captor = ArgumentCaptor.captor();

        when(accountService.getAccounts(argThat(argument -> argument != null && argument.getId() == 1L))).thenReturn(List.of(sourceAccount));
        when(accountService.getAccounts(argThat(argument -> argument != null && argument.getId() == 2L))).thenReturn(List.of(destinationAccount));
        when(accountService.charge(eq(sourceAccount.getId()), captor.capture())).thenReturn(true);
        when(accountService.makeTransfer(10L, 20L, BigDecimal.TEN)).thenReturn(true);

        boolean result = paymentProcessor.makeTransferWithComission(sourceAgreement, destinationAgreement, 0, 0, BigDecimal.TEN, BigDecimal.TEN);

        assertTrue(result);
        assertEquals(BigDecimal.ONE, captor.getValue());
    }

    @Test
    public void testMakeTransferWithComissionNotFound() {
        when(accountService.getAccounts(argThat(argument -> argument != null && argument.getId() == 1L))).thenReturn(List.of(sourceAccount));
        when(accountService.getAccounts(argThat(argument -> argument != null && argument.getId() == 2L))).thenReturn(new ArrayList<>());

        AccountException result = assertThrows(AccountException.class, () -> paymentProcessor.makeTransferWithComission(sourceAgreement, destinationAgreement,
                0, 0, BigDecimal.TEN, BigDecimal.TEN));

        assertEquals("Account not found", result.getMessage());

        result = assertThrows(AccountException.class, () -> paymentProcessor.makeTransferWithComission(destinationAgreement, sourceAgreement,
                0, 0, BigDecimal.TEN, BigDecimal.TEN));

        assertEquals("Account not found", result.getMessage());

        verify(accountService, times(3)).getAccounts(any());
    }
}
