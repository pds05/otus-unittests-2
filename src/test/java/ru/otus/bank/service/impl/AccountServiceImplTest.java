package ru.otus.bank.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.bank.dao.AccountDao;
import ru.otus.bank.entity.Account;
import ru.otus.bank.entity.Agreement;
import ru.otus.bank.service.exception.AccountException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {
    @Mock
    AccountDao accountDao;

    @InjectMocks
    AccountServiceImpl accountServiceImpl;

    @Test
    public void testTransfer() {
        Account sourceAccount = new Account();
        sourceAccount.setAmount(new BigDecimal(100));

        Account destinationAccount = new Account();
        destinationAccount.setAmount(new BigDecimal(10));

        when(accountDao.findById(eq(1L))).thenReturn(Optional.of(sourceAccount));
        when(accountDao.findById(eq(2L))).thenReturn(Optional.of(destinationAccount));

        accountServiceImpl.makeTransfer(1L, 2L, new BigDecimal(10));

        assertEquals(new BigDecimal(90), sourceAccount.getAmount());
        assertEquals(new BigDecimal(20), destinationAccount.getAmount());
    }

    @Test
    public void testSourceNotFound() {
        when(accountDao.findById(any())).thenReturn(Optional.empty());

        AccountException result = assertThrows(AccountException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                accountServiceImpl.makeTransfer(1L, 2L, new BigDecimal(10));
            }
        });
        assertEquals("No source account", result.getLocalizedMessage());
    }


    @Test
    public void testTransferWithVerify() {
        Account sourceAccount = new Account();
        sourceAccount.setAmount(new BigDecimal(100));
        sourceAccount.setId(1L);

        Account destinationAccount = new Account();
        destinationAccount.setAmount(new BigDecimal(10));
        destinationAccount.setId(2L);

        when(accountDao.findById(eq(1L))).thenReturn(Optional.of(sourceAccount));
        when(accountDao.findById(eq(2L))).thenReturn(Optional.of(destinationAccount));

        ArgumentMatcher<Account> sourceMatcher =
                argument -> argument.getId().equals(1L) && argument.getAmount().equals(new BigDecimal(90));

        ArgumentMatcher<Account> destinationMatcher =
                argument -> argument.getId().equals(2L) && argument.getAmount().equals(new BigDecimal(20));

        accountServiceImpl.makeTransfer(1L, 2L, new BigDecimal(10));

        verify(accountDao).save(argThat(sourceMatcher));
        verify(accountDao).save(argThat(destinationMatcher));
    }

    @Test
    public void testAddAccount() {
        Agreement agreement1 = new Agreement();
        agreement1.setId(1L);
        agreement1.setName("AG1");

        Agreement agreement2 = new Agreement();
        agreement2.setId(2L);
        agreement2.setName("AG2");

        when(accountDao.save(any())).thenAnswer(invocationOnMock -> {
            Account oldAccount = invocationOnMock.getArgument(0);
            Account newAccount = new Account();
            newAccount.setId(oldAccount.getAgreementId() + 100);
            newAccount.setType(oldAccount.getType());
            newAccount.setAmount(oldAccount.getAmount());
            newAccount.setAgreementId(oldAccount.getAgreementId());
            newAccount.setNumber(oldAccount.getNumber());
            return newAccount;
        });

        Account result = accountServiceImpl.addAccount(agreement1, "AC1", 1, new BigDecimal(100));

        assertEquals(1L, (long) result.getAgreementId());
        assertEquals(101L, (long) result.getId());
        assertEquals(new BigDecimal(100), result.getAmount());

        verify(accountDao).save(argThat(account -> account.getId().equals(0L) && account.getAgreementId().equals(1L)));

        result = accountServiceImpl.addAccount(agreement2, "AC2", 1, new BigDecimal(200));

        assertEquals(2L, result.getAgreementId());
        assertEquals(102L, result.getId());
        assertEquals(new BigDecimal(200), result.getAmount());

        verify(accountDao).save(argThat(account -> account.getId().equals(0L) && account.getAgreementId().equals(2L)));
    }

    @Test
    public void testGetAccounts() {
        when(accountDao.findAll()).thenReturn(() -> {
            List<Account> accounts = new ArrayList<>();
            Account account1 = new Account();
            account1.setId(1L);
            accounts.add(account1);

            Account account2 = new Account();
            account2.setId(2L);
            accounts.add(account2);

            Account account3 = new Account();
            account3.setId(3L);
            accounts.add(account3);
            return accounts.iterator();
        });

        assertNotNull(accountServiceImpl.getAccounts());
        assertEquals(3, accountServiceImpl.getAccounts().size());
        assertEquals(1L, accountServiceImpl.getAccounts().get(0).getId());
    }

    @Test
    public void testGetAccountsByAgreementId() {
        List<Account> accounts = new ArrayList<>();

        Agreement agreement = new Agreement();
        long agreementId = 101L;
        agreement.setId(agreementId);

        Account account1 = new Account();
        account1.setId(1L);
        account1.setAgreementId(agreementId);
        account1.setAmount(new BigDecimal(100));
        accounts.add(account1);

        Account account2 = new Account();
        account2.setId(2L);
        account2.setAgreementId(agreementId);
        account2.setAmount(new BigDecimal(200));
        accounts.add(account2);

        when(accountDao.findByAgreementId(agreementId)).thenReturn(accounts);

        List<Account> result = accountServiceImpl.getAccounts(agreement);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal(100), result.get(0).getAmount());
        assertEquals(new BigDecimal(200), result.get(1).getAmount());
    }

    @Test
    public void testCharge() {
        long accountId = 1L;
        BigDecimal amount = new BigDecimal(100);
        BigDecimal chargeAmount = new BigDecimal(20);

        ArgumentCaptor<Account> captor = ArgumentCaptor.captor();
        when(accountDao.save(captor.capture())).thenReturn(null);

        Account account = new Account();
        account.setId(accountId);
        account.setAmount(amount);

        when(accountDao.findById(eq(accountId))).thenReturn(Optional.of(account));
        BigDecimal expectedAmount = amount.subtract(chargeAmount);

        boolean result = accountServiceImpl.charge(accountId, chargeAmount);

        assertTrue(result);
        assertEquals(expectedAmount, captor.getValue().getAmount());

        verify(accountDao).save(argThat(argument -> argument.getId().equals(accountId) && argument.getAmount().equals(expectedAmount)));
    }

    @Test
    public void testChargeNotFound() {
        when(accountDao.findById(any())).thenReturn(Optional.empty());

        AccountException result = assertThrows(AccountException.class, () -> accountServiceImpl.charge(1L, new BigDecimal(100)));
        assertEquals("No source account", result.getMessage());

        verify(accountDao).findById(argThat(a -> a.equals(1L)));
    }
}
