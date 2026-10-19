import dataprovider.BookingDataProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DataProviderValidationTest {

    @Test
    public void shouldLoadCreateBookingDataFromExcel() throws Exception {
        Object[][] data = BookingDataProvider.bookingData();

        Assert.assertNotNull(data, "Excel data should not be null");
        Assert.assertTrue(
                data.length > 0,
                "Excel should contain at least one test data row"
        );
        Assert.assertEquals(
                data[0].length,
                7,
                "Each booking data row should contain 7 columns"
        );

        System.out.println("Excel data loaded successfully");
        System.out.println("Test data rows: " + data.length);
        System.out.println("Columns per row: " + data[0].length);
    }
}