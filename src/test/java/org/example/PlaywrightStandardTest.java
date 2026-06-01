package org.example;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PlaywrightStandardTest {

    Playwright playwright;
    Browser browser;
    BrowserContext context;
    Page page;
    Path folderPath;
    ConfigReader configReader;
    ExtentTest test;
    ExtentReports extent;
    List<Map<String, String>> rows;
    Properties config;

    @BeforeEach
    public void setupBrowser(TestInfo testInfo) throws IOException {

        //launch browser
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false));
        context = browser.newContext();
        page = context.newPage();

        //initialize Properties file
        config = ConfigReader.loadProperties();

        // Get values from config file
        String baseUrl = config.getProperty("base.url");
        page.navigate(baseUrl);
        configReader = new ConfigReader();
        folderPath= configReader.createFolder();

        String methodName = testInfo.getTestMethod().get().getName();
        // Reporting
        ExtentSparkReporter htmlReporter = new ExtentSparkReporter(folderPath+"/"+ methodName +"-report.html");
        extent = new ExtentReports();
        extent.attachReporter(htmlReporter);

        test = extent.createTest("Playwright Test", "Sample test with ExtentReports");

    }

    public void takeScreenshot(String screenshotName, Path folderPath){
        try {
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(folderPath+"/"+screenshotName+" fullpage.png"))
                    .setFullPage(true));
        } catch (Exception e) {
            System.err.println("Error taking full-page screenshot: " + e.getMessage());
        }
    }

    @Test
    public void testStandardFlow() throws IOException {
        csvFormat("standardFlowData.csv");
        userLogin();
        addProductsToCart();
        fillCustomerInfo();
        completePurchase();
        logOut();

    }

    @Test
    public void testDifferentFilters() throws IOException {
        csvFormat("standardFlowData.csv");
        userLogin();
        filtersAvailable();
        logOut();
    }

    // negative tests
    @Test
    public void testLockedOutUser() throws IOException {
        csvFormat("loginErrorData.csv");
        lockedOutUserLogin();
    }

    @Test
    public void testIncorrectUserDetails() throws IOException {
        csvFormat("loginErrorData.csv");
        incorrectUserLogin();
    }

    @Test
    public void testCancelCheckout() throws IOException {
        csvFormat("standardFlowData.csv");
        userLogin();
        addProductsToCart();
        fillCustomerInfo();
        cancelCheckout();
        logOut();
    }

    @Test
    public void testInputValidation() throws IOException {
        csvFormat("inputValidationData.csv");
        userLogin();
        addProductsToCart();
        //test for empty firstName
        checkOutFormFirstNameValidation();
        clearInputField();


        //test for empty lastname
        checkOutFormLastNameValidation();
        clearInputField();

        //test for empty zip code
        checkOutFormZipCodeValidation();
        clearInputField();

        // test for incorrect input validation
        checkOutFormInputValidation();
        completePurchase();
        logOut();
    }

    @AfterEach
    public void flushReporter(){
        extent.flush();
    }

    public void userLogin() throws IOException {

        // Get values from config file
        page.locator("[data-test=\"username\"]").click();
        page.locator("[data-test=\"username\"]").fill(config.getProperty("standard_username"));
        page.locator("[data-test=\"password\"]").click();
        page.locator("[data-test=\"password\"]").fill(config.getProperty("standard_password"));
        takeScreenshot("Successfully entered login details",folderPath);
        page.locator("[data-test=\"login-button\"]").click();
        takeScreenshot("Successfully Logged In",folderPath);

        test.pass("Logged in success");
    }

    public void addProductsToCart(){
        assertThat(page.locator("[data-test=\"item-4-title-link\"] [data-test=\"inventory-item-name\"]")).containsText("Sauce Labs Backpack");
        page.locator("[data-test=\"add-to-cart-sauce-labs-backpack\"]").click();
        page.locator("[data-test=\"add-to-cart-sauce-labs-bike-light\"]").click();
        page.locator("[data-test=\"add-to-cart-sauce-labs-bolt-t-shirt\"]").click();
        page.locator("[data-test=\"add-to-cart-sauce-labs-fleece-jacket\"]").click();
        page.locator("[data-test=\"add-to-cart-sauce-labs-onesie\"]").click();
        page.locator("[data-test=\"add-to-cart-test\\.allthethings\\(\\)-t-shirt-\\(red\\)\"]").click();
        takeScreenshot("To show that Card has 6 items",folderPath);
        page.locator("[data-test=\"shopping-cart-link\"]").click();
        page.locator("[data-test=\"checkout\"]").click();
        test.pass("Added all products and checked out");
    }

    public void fillCustomerInfo(){
        page.locator("[data-test=\"firstName\"]").click();
        page.locator("[data-test=\"firstName\"]").fill(rows.get(0).get("firstName"));
        page.locator("[data-test=\"lastName\"]").click();
        page.locator("[data-test=\"lastName\"]").fill(rows.get(0).get("lastName"));
        page.locator("[data-test=\"postalCode\"]").click();
        page.locator("[data-test=\"postalCode\"]").fill(rows.get(0).get("zipCode"));
        takeScreenshot("All customer information added",folderPath);
        page.locator("[data-test=\"continue\"]").click();
        test.pass("All customer information added");
    }

    public void completePurchase(){
        assertThat(page.locator("[data-test=\"subtotal-label\"]")).isVisible();
        assertThat(page.locator("[data-test=\"total-label\"]")).isVisible();
        takeScreenshot("Purchase Information",folderPath);
        page.locator("[data-test=\"finish\"]").click();
        assertThat(page.locator("[data-test=\"complete-header\"]")).containsText("Thank you for your order!");
        takeScreenshot("Thank you for your order",folderPath);

        test.pass(" Thank you for your order! displayed");
        page.locator("[data-test=\"back-to-products\"]").click();
    }

    public void logOut(){
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open Menu")).click();
        assertThat(page.locator("[data-test=\"logout-sidebar-link\"]")).isVisible();
        page.locator("[data-test=\"logout-sidebar-link\"]").click();
        test.pass("Successfully logged out");
    }

    public void lockedOutUserLogin(){

        page.locator("[data-test=\"username\"]").click();
        page.locator("[data-test=\"username\"]").fill(config.getProperty("locked_username"));
        page.locator("[data-test=\"password\"]").click();
        page.locator("[data-test=\"password\"]").fill(config.getProperty("locked_password"));
        page.locator("[data-test=\"login-button\"]").click();
        page.locator("[data-test=\"error\"]").first().innerText();
        System.out.println(page.locator("[data-test=\"error\"]").first().innerText());
        takeScreenshot("User Login Error Message Displayed",folderPath);
        test.pass("User Login Error Message Displayed");
    }
    public void incorrectUserLogin(){

        page.locator("[data-test=\"username\"]").click();
        page.locator("[data-test=\"username\"]").fill(config.getProperty("incorrect_username"));
        page.locator("[data-test=\"password\"]").click();
        page.locator("[data-test=\"password\"]").fill(config.getProperty("incorrect_password"));
        page.locator("[data-test=\"login-button\"]").click();
        page.locator("[data-test=\"error\"]").first().innerText();
        System.out.println(page.locator("[data-test=\"error\"]").first().innerText());
        takeScreenshot("User Login Error Message Displayed",folderPath);
        test.pass("User Login Error Message Displayed");
    }

    public void filtersAvailable(){
        page.locator("[data-test=\"product-sort-container\"]").selectOption("za");
        takeScreenshot("Products ordered from Z to A",folderPath);
        page.locator("[data-test=\"product-sort-container\"]").selectOption("az");
        takeScreenshot("Products ordered from A to Z",folderPath);
        page.locator("[data-test=\"product-sort-container\"]").selectOption("lohi");
        takeScreenshot("Products ordered by Price from low to high",folderPath);
        page.locator("[data-test=\"product-sort-container\"]").selectOption("hilo");
        takeScreenshot("Products ordered by Price from high to low",folderPath);
        test.pass("Completed all filtering options");
    }

    public void cancelCheckout(){

        page.locator("[data-test=\"cancel\"]").click();
        page.locator("[data-test=\"shopping-cart-link\"]").click();
        takeScreenshot("Cart number before removing Items",folderPath);
        page.locator("[data-test=\"remove-sauce-labs-backpack\"]").click();
        page.locator("[data-test=\"remove-sauce-labs-bike-light\"]").click();
        page.locator("[data-test=\"remove-sauce-labs-bolt-t-shirt\"]").click();
        page.locator("[data-test=\"remove-sauce-labs-fleece-jacket\"]").click();
        page.locator("[data-test=\"remove-sauce-labs-onesie\"]").click();
        page.locator("[data-test=\"remove-test\\.allthethings\\(\\)-t-shirt-\\(red\\)\"]").click();
        takeScreenshot("Cart number after removing Items",folderPath);
        test.pass("Successfully Removed al products from cart");
        page.locator("[data-test=\"continue-shopping\"]").click();
    }

    public void checkOutFormFirstNameValidation(){

        page.locator("[data-test=\"lastName\"]").click();
        page.locator("[data-test=\"lastName\"]").fill(rows.get(0).get("lastName"));
        page.locator("[data-test=\"postalCode\"]").click();
        page.locator("[data-test=\"postalCode\"]").fill(rows.get(0).get("zipCode"));
        page.locator("[data-test=\"continue\"]").click();
        assertThat(page.locator("[data-test=\"error\"]")).containsText("Error: First Name is required");
        takeScreenshot("Error First Name is required",folderPath);
        test.pass("Error: First Name is required");

    }
    public void checkOutFormLastNameValidation(){

        page.locator("[data-test=\"firstName\"]").click();
        page.locator("[data-test=\"firstName\"]").fill(rows.get(1).get("firstName"));
        page.locator("[data-test=\"postalCode\"]").click();
        page.locator("[data-test=\"postalCode\"]").fill(rows.get(1).get("zipCode"));
        page.locator("[data-test=\"continue\"]").click();
        assertThat(page.locator("[data-test=\"error\"]")).containsText("Error: Last Name is required");
        takeScreenshot("Error Last Name is required",folderPath);
        test.pass("Error: Last Name is required");
    }
    public void checkOutFormZipCodeValidation(){

        page.locator("[data-test=\"firstName\"]").click();
        page.locator("[data-test=\"firstName\"]").fill(rows.get(2).get("firstName"));
        page.locator("[data-test=\"lastName\"]").click();
        page.locator("[data-test=\"lastName\"]").fill(rows.get(2).get("lastName"));
        page.locator("[data-test=\"continue\"]").click();
        assertThat(page.locator("[data-test=\"error\"]")).containsText("Error: Postal Code is required");
        takeScreenshot("Error Postal Code is required",folderPath);
        test.pass("Error: Postal Code is required");
    }
    public void checkOutFormInputValidation(){

        page.locator("[data-test=\"firstName\"]").click();
        page.locator("[data-test=\"firstName\"]").fill(rows.get(3).get("firstName"));
        page.locator("[data-test=\"lastName\"]").click();
        page.locator("[data-test=\"lastName\"]").fill(rows.get(3).get("lastName"));
        page.locator("[data-test=\"postalCode\"]").click();
        page.locator("[data-test=\"postalCode\"]").fill(rows.get(3).get("zipCode"));
        assertThat(page.locator("[data-test=\"firstName\"]")).hasValue("8945452");
        assertThat(page.locator("[data-test=\"lastName\"]")).hasValue("645484");
        assertThat(page.locator("[data-test=\"postalCode\"]")).hasValue("two thousand and one");
        test.fail("Incorrect data type of input fields entered");
        takeScreenshot("Input validation failed to stop test",folderPath);
        page.locator("[data-test=\"continue\"]").click();
    }
    public void clearInputField(){
        page.locator("[data-test=\"firstName\"]").click();
        page.locator("[data-test=\"firstName\"]").fill("");
        page.locator("[data-test=\"lastName\"]").click();
        page.locator("[data-test=\"lastName\"]").fill("");
        page.locator("[data-test=\"postalCode\"]").click();
        page.locator("[data-test=\"postalCode\"]").fill("");
    }

    public List csvFormat(String dataFile)throws IOException {
        Reader in = new FileReader("src/test/resources/inputData/"+dataFile);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(in);

        // Store all rows in an ArrayList
        rows = new ArrayList<>();

        for (CSVRecord record : records) {
            rows.add(record.toMap());
        }
        return rows;
    }
}