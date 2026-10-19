
package tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import api.AuthenticationAPI;
import api.BookingAPI;
import builders.BookingBuilder;
import dataprovider.BookingDataProvider;
import io.restassured.response.Response;
import models.Booking;
import validators.ResponseValidator;

public class GetBookingTest {

    @Test(
        dataProvider = "bookingData",
        dataProviderClass = BookingDataProvider.class
    )
    public void verifyGetBooking(
            String firstName,
            String lastName,
            int price,
            String checkIn,
            String checkOut,
            boolean depositPaid,
            String additionalNeeds) {

        int bookingId = 0;
        String token = null;

        try {
            // 1. 创建预约
            Booking booking = BookingBuilder.createBooking(
                    firstName,
                    lastName,
                    price,
                    checkIn,
                    checkOut,
                    depositPaid,
                    additionalNeeds
            );

            bookingId = BookingAPI.createBookingAndReturnId(booking);

            // 2. 查询预约
            Response response = BookingAPI.getBooking(bookingId);

            ResponseValidator.validateStatusCode(response, 200);
            ResponseValidator.validateContentType(response);
            ResponseValidator.validateResponseTime(response);
            ResponseValidator.validateSchema(
                    response,
                    "schema/booking-schema.json"
            );

            Booking actualBooking = response.as(Booking.class);

            ResponseValidator.validateBooking(booking, actualBooking);

        } finally {
            // 3. 清理测试数据
            if (bookingId > 0) {
                try {
                    if (token == null) {
                        token = AuthenticationAPI.generateToken();
                    }

                    Response deleteResponse =
                            BookingAPI.deleteBooking(bookingId, token);

                    Assert.assertEquals(
                            deleteResponse.getStatusCode(),
                            201,
                            "清理测试预约失败，bookingId=" + bookingId
                    );

                } catch (Exception e) {
                    throw new AssertionError(
                            "清理测试预约时发生异常，bookingId=" + bookingId,
                            e
                    );
                }
            }
        }
    }
}
