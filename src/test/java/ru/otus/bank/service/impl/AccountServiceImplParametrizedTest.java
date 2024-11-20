package ru.otus.bank.service.impl;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.bank.dao.AccountDao;
import ru.otus.bank.entity.Account;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplParametrizedTest {
    @Mock
    AccountDao accountDao;

    @InjectMocks
    AccountServiceImpl accountServiceImpl;

    @ParameterizedTest
    @CsvSource({"1, 100, 10, 90, true", "2, 0, 100, -100, true"})
    public void testCharge(long accountId, BigDecimal amount, BigDecimal chargeAmount, BigDecimal expectedAmount, boolean expectedResult) {
        ArgumentCaptor<Account> captor = ArgumentCaptor.captor();
        when(accountDao.save(captor.capture())).thenReturn(null);

        Account account = new Account();
        account.setId(accountId);
        account.setAmount(amount);

        when(accountDao.findById(any())).thenReturn(Optional.of(account));
        boolean result = accountServiceImpl.charge(accountId, chargeAmount);

        assertEquals(expectedResult, result);
        assertEquals(expectedAmount, captor.getValue().getAmount());

        verify(accountDao).save(argThat(argument -> argument.getId().equals(accountId) && argument.getAmount().equals(expectedAmount)));
    }

    @ParameterizedTest
    @CsvSource({"100, 10, true", "10, 100, false", "10, 0, false", "10, -1, false"})
    public void testTransferValidation(String sourceSum, String transferSum, String expectedResult) {
        BigDecimal sourceAmount = new BigDecimal(sourceSum);
        BigDecimal transferAmount = new BigDecimal(transferSum);
        Boolean expected = Boolean.parseBoolean(expectedResult);

        Account sourceAccount = new Account();
        sourceAccount.setAmount(sourceAmount);
        sourceAccount.setId(1L);

        Account destinationAccount = new Account();
        destinationAccount.setAmount(new BigDecimal(10));
        destinationAccount.setId(2L);

        when(accountDao.findById(eq(1L))).thenReturn(Optional.of(sourceAccount));
        when(accountDao.findById(eq(2L))).thenReturn(Optional.of(destinationAccount));

        assertEquals(expected, accountServiceImpl.makeTransfer(1L, 2L, transferAmount));
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    public void testTransferValidationMethodSource(BigDecimal sourceAmount, BigDecimal transferAmount, Boolean expected) {
        Account sourceAccount = new Account();
        sourceAccount.setAmount(sourceAmount);
        sourceAccount.setId(1L);

        Account destinationAccount = new Account();
        destinationAccount.setAmount(new BigDecimal(10));
        destinationAccount.setId(2L);

        when(accountDao.findById(eq(1L))).thenReturn(Optional.of(sourceAccount));
        when(accountDao.findById(eq(2L))).thenReturn(Optional.of(destinationAccount));

        assertEquals(expected, accountServiceImpl.makeTransfer(1L, 2L, transferAmount));
    }

    public static Stream<? extends Arguments> provideParameters() {
        return Stream.of(
                Arguments.of(new BigDecimal(100), new BigDecimal(10), true),
                Arguments.of(new BigDecimal(10), new BigDecimal(100), false),
                Arguments.of(new BigDecimal(100), new BigDecimal(0), false),
                Arguments.of(new BigDecimal(100), new BigDecimal(-1), false)
        );
    }
}
