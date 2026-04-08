package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import com.mazurek.eventOrganizer.testData.TestConstants;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CityService unit tests:")
class CityServiceUnitTest {

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CityService cityService;

    @Nested
    @DisplayName("Get city by name tests:")
    class GetCityByNameTests {

        @Test
        @DisplayName("When getting city by name should return dto with correct data")
        void whenCityExistsShouldReturnDtoWithCorrectData() {
            City storedCity = CityTestBuilder.warsaw().build();
            when(cityRepository.findByIgnoreCaseName(TestConstants.CitiesConstants.WARSAW_NAME))
                    .thenReturn(Optional.of(storedCity));

            CityDto result = cityService.getCityByName(TestConstants.CitiesConstants.WARSAW_NAME);

            assertThat(result.getId()).isEqualTo(TestConstants.CitiesConstants.WARSAW_ID);
            assertThat(result.getName()).isEqualTo(TestConstants.CitiesConstants.WARSAW_NAME);
        }

        @Test
        @DisplayName("When getting city by name should throw CityNotFoundException if city does not exist")
        void whenCityDoesNotExistShouldThrowCityNotFoundException() {
            when(cityRepository.findByIgnoreCaseName(TestConstants.CitiesConstants.SYSTEM_CITY_NAME))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> cityService.getCityByName(TestConstants.CitiesConstants.SYSTEM_CITY_NAME))
                    .isInstanceOf(CityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get city by name or create tests:")
    class GetCityByNameOrCreateTests {

        @Test
        @DisplayName("When city name is null should throw IllegalArgumentException")
        void whenCityNameIsNullShouldThrowIllegalArgumentException() {
            assertThatThrownBy(() -> cityService.getCityByNameOrCreate(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("When city name is blank should throw IllegalArgumentException")
        void whenCityNameIsBlankShouldThrowIllegalArgumentException() {
            assertThatThrownBy(() -> cityService.getCityByNameOrCreate(TestConstants.InvalidInputConstants.BLANK_VALUE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("When getting city by name or creating city should return existing city regardless of case")
        void whenCityExistsRegardlessOfCaseShouldReturnExistingCity() {
            City storedCity = CityTestBuilder.krakow().build();
            when(cityRepository.findByIgnoreCaseName(TestConstants.CitiesConstants.KRAKOW_NAME))
                    .thenReturn(Optional.of(storedCity));

            City result = cityService.getCityByNameOrCreate(TestConstants.CitiesConstants.KRAKOW_NAME.toUpperCase());

            assertThat(result.getId()).isEqualTo(storedCity.getId());
            verify(cityRepository, never()).save(any(City.class));
        }

        @Test
        @DisplayName("When getting city by name or creating city should normalize name and create city if it does not exist")
        void whenCityDoesNotExistShouldNormalizeNameAndCreateCity() {
            City savedCity = CityTestBuilder.krakow().build();
            when(cityRepository.findByIgnoreCaseName(TestConstants.CitiesConstants.KRAKOW_NAME))
                    .thenReturn(Optional.empty());
            when(cityRepository.save(any(City.class))).thenReturn(savedCity);

            City result = cityService.getCityByNameOrCreate(TestConstants.CitiesConstants.KRAKOW_NAME.toUpperCase());

            assertThat(result.getName()).isEqualTo(TestConstants.CitiesConstants.KRAKOW_NAME);
            verify(cityRepository, times(1)).save(any(City.class));
        }

        @Test
        @DisplayName("When save fails due to race condition should load city again and return it")
        void whenSaveFailsDueToRaceConditionShouldLoadCityAgainAndReturnIt() {
            City storedCity = CityTestBuilder.krakow().build();
            when(cityRepository.findByIgnoreCaseName(TestConstants.CitiesConstants.KRAKOW_NAME))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(storedCity));
            when(cityRepository.save(any(City.class)))
                    .thenThrow(new DataIntegrityViolationException(TestConstants.InvalidInputConstants.DUPLICATE_KEY_MESSAGE));

            City result = cityService.getCityByNameOrCreate(TestConstants.CitiesConstants.KRAKOW_NAME);

            assertThat(result.getId()).isEqualTo(storedCity.getId());
            verify(cityRepository, times(2)).findByIgnoreCaseName(TestConstants.CitiesConstants.KRAKOW_NAME);
        }
    }
}
