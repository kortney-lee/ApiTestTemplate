import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.testng.Assert.assertEquals;

//Assuming request return in body and not in URI
public class ApiTestTemplate {

    private String bearerToken;
    private Connection conn;

    //Assuming OAuth is being used
    @BeforeClass
    @Parameters({"authUrl", "authUsername", "authPassword", "dbUrl", "dbUser", "dbPassword"})
    public void setup(String authUrl, String authUsername, String authPassword, String dbUrl, String dbUser, String dbPassword) {
        // Perform authentication and get the bearer token
        // This is just an example, replace it with your actual authentication logic
        bearerToken = "your_bearer_token";

        // Set up database connection and only one connection to DB before test are execute to ensure only one session
        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test(description = "Test GET endpoint")
    @Parameters({"baseUrl", "endpoint"})
    public void testGetEndpoint(String baseUrl, String endpoint) {
        // Retrieve values from database that are need for the test
        String expectedValue = getValueFromDatabase();

        // Make API request to get
        Response response = given()
                .baseUri(baseUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .when()
                .get(endpoint)
                .then()
                .statusCode(200)
                .extract()
                .response();

        // Compares response body to value from database
        String responseBody = response.getBody().asString();
        assertEquals(responseBody, expectedValue, "Response body does not match value from database");
    }

    private String getValueFromDatabase() {
        String value = null;
        try {
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM your_table");
            if (resultSet.next()) {
                value = resultSet.getString("your_column");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return value;
    }

    @Test(description = "Test POST endpoint", dependsOnMethods = "apiTestStart")
    @Parameters({"baseUrl", "endpoint"})
    public void testPostEndpoint(String baseUrl, String endpoint) {
        String requestBody = "{ \"key\": \"value\" }";

        Response response = given()
                .baseUri(baseUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .body(requestBody)
                .when()
                .post(endpoint)
                .then()
                .statusCode(201)
                .extract()
                .response();

        // Compares response body to expected value
        String responseBody = response.getBody().asString();
        String expectedValue = "expectedValue"; // Change this to your expected value
        assertEquals(responseBody, expectedValue, "Response body does not match expected value");
    }


    @Test(description = "Test PUT endpoint", dependsOnMethods = "apiTestStart")
    @Parameters({"baseUrl", "endpoint"})
    public void testPutEndpoint(String baseUrl, String endpoint) {
        String requestBody = "{ \"key\": \"newValue\" }";

        given()
                .baseUri(baseUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .body(requestBody)
                .when()
                .put(endpoint + "/1")
                .then()
                .statusCode(200)
                .body("key", equalTo("newValue"));

        // Check if the record was updated in the database
        int updatedRecordCount = getRecordCountFromDatabase();
        assertEquals(updatedRecordCount, 1, "Record was not updated successfully");
    }

    private int getRecordCountFromDatabase() {
        int count = 0;
        try {
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) AS count FROM your_table");
            if (resultSet.next()) {
                count = resultSet.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    @Test(description = "Test DELETE endpoint", dependsOnMethods = "apiTestStart")
    @Parameters({"baseUrl", "endpoint"})
    public void testDeleteEndpoint(String baseUrl, String endpoint) {
        // Get the initial count of records in the table
        int initialRecordCount = getRecordCountFromDatabase();

        // Perform the DELETE request
        given()
                .baseUri(baseUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .when()
                .delete(endpoint + "/1")
                .then()
                .statusCode(204);

        // Get the count of records in the table after the DELETE request
        int finalRecordCount = getRecordCountFromDatabase();

        // Assert that the final record count is less than the initial count
        assertEquals(finalRecordCount, initialRecordCount - 1, "Record was not deleted successfully");
    }
}
