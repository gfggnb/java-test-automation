package dataprovider;

import org.testng.annotations.DataProvider;

import utilities.ConfigReader;
import utilities.ExcelUtils;

public class BookingDataProvider {

    private static final String FILE_PATH =
            ConfigReader.getProperty("testDataPath");

    @DataProvider(name = "bookingData")
    public static Object[][] bookingData() throws Exception {

        Object[][] data = ExcelUtils.getExcelData(
                FILE_PATH,
                "createBooking");

        for (Object[] row : data) {

            System.out.println("-----------");

            for (Object value : row) {

                System.out.println(
                        value + " --> " +
                        (value == null ? "null" : value.getClass().getName()));
            }
        }

        return data;
    }

    @DataProvider(name = "updateBookingData")
    public static Object[][] updateBookingData() throws Exception {

        return ExcelUtils.getExcelData(
                FILE_PATH,
                "UpdateBooking");
    }

    @DataProvider(name = "partialUpdateBookingData")
    public static Object[][] partialUpdateBookingData() throws Exception {

        return ExcelUtils.getExcelData(
                FILE_PATH,
                "PartialUpdateBooking");
    }
    

}
