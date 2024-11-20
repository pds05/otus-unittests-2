package ru.otus.bank.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.otus.bank.dao.AgreementDao;
import ru.otus.bank.entity.Agreement;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AgreementServiceImplTest {

    private AgreementDao dao = mock(AgreementDao.class);

    AgreementServiceImpl agreementServiceImpl;

    @BeforeEach
    public void init() {
        agreementServiceImpl = new AgreementServiceImpl(dao);
    }

    @Test
    public void testFindByName() {
        String name = "test";
        Agreement agreement = new Agreement();
        agreement.setId(10L);
        agreement.setName(name);

        when(dao.findByName(name)).thenReturn(
                Optional.of(agreement));

        Optional<Agreement> result = agreementServiceImpl.findByName(name);

        assertTrue(result.isPresent());
        assertEquals(10, agreement.getId());
    }

    @Test
    public void testFindByNameWithCaptor() {
        String name = "test";
        Agreement agreement = new Agreement();
        agreement.setId(10L);
        agreement.setName(name);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        when(dao.findByName(captor.capture())).thenReturn(
                Optional.of(agreement));

        Optional<Agreement> result = agreementServiceImpl.findByName(name);

        assertEquals("test", captor.getValue());
        assertTrue(result.isPresent());
        assertEquals(10, agreement.getId());
    }

    @Test
    public void addAgreementTest() {
        String name = "Agreement 1";

        when(dao.save(any())).thenAnswer(invocationOnMock -> {
            Agreement oldAgreement = invocationOnMock.getArgument(0);
            Agreement newAgreement = new Agreement();
            newAgreement.setId(1L);
            newAgreement.setName(oldAgreement.getName());
            return newAgreement;
        });
        Agreement result = agreementServiceImpl.addAgreement(name);

        assertEquals(1L, result.getId());

        verify(dao).save(argThat(agreement -> agreement.getId() == null && agreement.getName().equals(name)));
    }
}
