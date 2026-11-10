package dev.shrekback.shipping.service;

import dev.shrekback.shipping.dao.*;
import dev.shrekback.shipping.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SippingServiceImplTest {
    ShippingCountriesRepository countries = mock(ShippingCountriesRepository.class);
    ShippingEMSRepository ems = mock(ShippingEMSRepository.class);
    ShippingECORepository eco = mock(ShippingECORepository.class);
    SippingServiceImpl service = new SippingServiceImpl(countries, ems, eco, mock(MongoTemplate.class));

    @Test
    void eligibleEcoPostPriceIsNotOverwrittenByEms() {
        when(countries.findById("Israel")).thenReturn(Optional.of(new ShippingCountry("Israel", 1, 1)));
        EcoPost rate = new EcoPost(); rate.setPriceUpTo500g(30);
        when(eco.findById("1")).thenReturn(Optional.of(rate));
        assertEquals(30.0, service.getShippingCost("Israel", 500));
        verifyNoInteractions(ems);
    }

    @Test
    void fallsBackToEmsAboveEcoWeightAndRoundsPartialBlocksUp() {
        when(countries.findById("Israel")).thenReturn(Optional.of(new ShippingCountry("Israel", 1, 1)));
        Ems rate = new Ems(); rate.setPriceUpTo1000g(50); rate.setEachAdditional500g(10);
        when(ems.findById("1")).thenReturn(Optional.of(rate));
        assertEquals(80.0, service.getShippingCost("Israel", 2001));
        verifyNoInteractions(eco);
    }

    @Test
    void invalidWeightsAndUnsupportedParcelsNeverReturnFreeShipping() {
        for (double weight : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY, 30001}) {
            assertThrows(ResponseStatusException.class, () -> service.getShippingCost("Israel", weight));
        }
        when(countries.findById("Unsupported")).thenReturn(Optional.of(new ShippingCountry("Unsupported", 0, 0)));
        assertThrows(ResponseStatusException.class, () -> service.getShippingCost("Unsupported", 100));
    }
}
