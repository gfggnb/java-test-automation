
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

public class DeleteBookingTest {

    @Test(
            dataProvider = "bookingData",
            dataProviderClass = BookingDataProvider.class
    )
    public void verifyDeleteBookingRequest(
            String firstName,
            String lastName,
            int price,
            String checkIn,
            String checkOut,
            boolean depositPaid,
            String additionalNeeds) {

        int bookingId = 0;
        String token = null;
        boolean deleteSucceeded = false;

        try {
            // 1. 创建预约
            Booking booking =
                    BookingBuilder.createBooking(
                            firstName,
                            lastName,
                            price,
                            checkIn,
                            checkOut,
                            depositPaid,
                            additionalNeeds
                    );

            bookingId =
                    BookingAPI.createBookingAndReturnId(booking);

            // 2. 获取认证 Token
            token = AuthenticationAPI.generateToken();

            // 3. 删除预约
            Response deleteResponse =
                    BookingAPI.deleteBooking(bookingId, token);

            ResponseValidator.validateStatusCode(deleteResponse, 201);
            deleteSucceeded = true;

            // 4. 验证预约已不存在
            Response getResponse = BookingAPI.getBooking(bookingId);

            ResponseValidator.validateStatusCode(getResponse, 404);

        } finally {
            // 5. 如果前面的删除未成功，尝试清理预约
            if (bookingId > 0 && !deleteSucceeded) {
                try {
                    if (token == null) {
                        token = AuthenticationAPI.generateToken();
                    }

                    Response cleanupResponse =
                            BookingAPI.deleteBooking(bookingId, token);

                    int statusCode = cleanupResponse.getStatusCode();

                    // 201 表示删除成功；404 表示预约已经不存在
                    Assert.assertTrue(
                            statusCode == 201 || statusCode == 404,
                            "清理预约失败，bookingId=" + bookingId
                                    + "，HTTP 状态码=" + statusCode
                    );

                } catch (Exception e) {
                    throw new AssertionError(
                            "清理预约时发生异常，bookingId=" + bookingId,
                            e
                    );
                }
            }
        }
    }
}
