
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

public class UpdateBookingTest {

    @Test(
            dataProvider = "updateBookingData",
            dataProviderClass = BookingDataProvider.class
    )
    public void verifyUpdateBookingRequest(
            String originalFirstName,
            String originalLastName,
            int originalPrice,
            String checkIn,
            String checkOut,
            boolean depositPaid,
            String additionalNeeds,
            String updatedFirstName,
            String updatedLastName,
            int updatedPrice,
            String updatedAdditionalNeeds) {

        int bookingId = 0;
        String token = null;

        try {
            // 1. 创建原始预约
            Booking originalBooking =
                    BookingBuilder.createBooking(
                            originalFirstName,
                            originalLastName,
                            originalPrice,
                            checkIn,
                            checkOut,
                            depositPaid,
                            additionalNeeds
                    );

            bookingId =
                    BookingAPI.createBookingAndReturnId(originalBooking);

            // 2. 构造更新后的预约
            Booking updatedBooking =
                    BookingBuilder.createBooking(
                            updatedFirstName,
                            updatedLastName,
                            updatedPrice,
                            checkIn,
                            checkOut,
                            depositPaid,
                            updatedAdditionalNeeds
                    );

            // 3. 获取认证 Token
            token = AuthenticationAPI.generateToken();

            // 4. 发送 PUT 更新请求
            Response response =
                    BookingAPI.updateBooking(
                            bookingId,
                            token,
                            updatedBooking
                    );

            // 5. 校验响应
            ResponseValidator.validateStatusCode(response, 200);
            ResponseValidator.validateContentType(response);
            ResponseValidator.validateResponseTime(response);
            ResponseValidator.validateSchema(
                    response,
                    "schema/booking-schema.json"
            );

            // 6. 校验更新后的业务数据
            Booking actualBooking = response.as(Booking.class);

            ResponseValidator.validateBooking(
                    updatedBooking,
                    actualBooking
            );

        } finally {
            // 7. 无论测试成功还是失败，都尝试清理已创建的预约
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
