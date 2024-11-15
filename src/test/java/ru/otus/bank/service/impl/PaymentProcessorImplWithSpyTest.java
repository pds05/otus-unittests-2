package ru.otus.bank.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.verification.VerificationMode;
import ru.otus.bank.dao.AccountDao;
import ru.otus.bank.entity.Account;
import ru.otus.bank.entity.Agreement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class PaymentProcessorImplWithSpyTest {
    Agreement sourceAgreement;
    Agreement destinationAgreement;
    Account sourceAccount;
    Account destinationAccount;

    @Mock
    AccountDao accountDao;

    @Spy
    @InjectMocks
    AccountServiceImpl accountService;

    @InjectMocks
    PaymentProcessorImpl paymentProcessor;

    @BeforeEach
    public void init() {
        paymentProcessor = new PaymentProcessorImpl(accountService);

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

    @CsvSource({"10, true", "0, false", "200, false"})
    @ParameterizedTest
    public void testTransfer(BigDecimal transferAmount, boolean expectedResult) {
        ArgumentCaptor<Account> captor = ArgumentCaptor.captor();
        lenient().when(accountDao.save(captor.capture())).thenReturn(null);
        when(accountDao.findById(10L)).thenReturn(Optional.of(sourceAccount));
        when(accountDao.findById(20L)).thenReturn(Optional.of(destinationAccount));

        doReturn(List.of(sourceAccount)).when(accountService).getAccounts(argThat(argument -> argument != null && argument.getId() == 1L));
        doReturn(List.of(destinationAccount)).when(accountService).getAccounts(argThat(argument -> argument != null && argument.getId() == 2L));

        when(accountService.makeTransfer(10L, 20L, transferAmount)).thenCallRealMethod();

        boolean result = paymentProcessor.makeTransfer(sourceAgreement, destinationAgreement,
                0, 0, transferAmount);

        assertEquals(expectedResult, result);
        if (result) {
            assertEquals(new BigDecimal(80), captor.getAllValues().get(2).getAmount());
            assertEquals(new BigDecimal(20), captor.getAllValues().get(3).getAmount());
        }
        verify(accountService, times(2)).getAccounts(argThat(argument -> argument != null && (argument.getId() == 1L || argument.getId() == 2L)));
        verify(accountService).makeTransfer(10L, 20L, transferAmount);
    }

    @Test
    public void testTransferWithComission() {
        BigDecimal transferAmount = BigDecimal.TEN;
        BigDecimal comissionPercent = BigDecimal.TEN;

        ArgumentCaptor<Account> captor = ArgumentCaptor.captor();
        lenient().when(accountDao.save(captor.capture())).thenReturn(null);
        when(accountDao.findById(10L)).thenReturn(Optional.of(sourceAccount));
        when(accountDao.findById(20L)).thenReturn(Optional.of(destinationAccount));

        doReturn(List.of(sourceAccount)).when(accountService).getAccounts(argThat(argument -> argument != null && argument.getId() == 1L));
        doReturn(List.of(destinationAccount)).when(accountService).getAccounts(argThat(argument -> argument != null && argument.getId() == 2L));

        when(accountService.charge(10L, BigDecimal.ONE)).thenCallRealMethod();
        when(accountService.makeTransfer(10L, 20L, transferAmount)).thenCallRealMethod();

        boolean result = paymentProcessor.makeTransferWithComission(sourceAgreement, destinationAgreement, 0, 0, transferAmount, comissionPercent);

        assertTrue(result);
        assertEquals(new BigDecimal(78), captor.getAllValues().get(3).getAmount());
        assertEquals(new BigDecimal(20), captor.getAllValues().get(5).getAmount());

        verify(accountService, times(2)).getAccounts(argThat(argument -> argument != null && (argument.getId() == 1L || argument.getId() == 2L)));
        verify(accountService, times(1)).charge(10L, BigDecimal.ONE);
        verify(accountService).makeTransfer(10L, 20L, transferAmount);
    }
}
