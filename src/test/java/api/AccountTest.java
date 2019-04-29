package api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import lib.Base;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertTrue;

public class AccountTest {
    Base base;
    String organization ;
    String saltValue ;
    String iteration ;
    String vendorId;
    RequestSpecification requestSpec;
    RequestSpecification requestSpecVersion2;

    @BeforeClass
    public void beforeClass(){
        base = new Base();
        base.loadConfig();
        base.loadTestData();
        organization = base.getConfig("orgname");
        saltValue = base.getConfig("salt");
        iteration = base.getConfig("iteration");
        vendorId = base.getConfig("vendorId");
        requestSpec = new RequestSpecBuilder().setBaseUri(base.getConfig("baseURI")).build();
        requestSpecVersion2 = new RequestSpecBuilder().setBaseUri(base.getConfig("baseURI2")).build();
    }
    @DataProvider(name="encrypt")
    public Object[][] encryptkey(){
        Map<String,Object> queryParams = new HashMap<String,Object>();
        queryParams.put("toBeEncrypted",base.getTestData("account.number"));
        queryParams.put("secretKey",saltValue);
        queryParams.put("iterations",iteration);
        Response response=given().
                spec(requestSpec).queryParameters(queryParams).
                when().
                get("encrypt/"+organization);
        return new Object[][]{{response.body().asString()}};
    }
    @Test(dataProvider="encrypt")
    public void accountCreate(String id){
        Map<String,Object> queryParams = new HashMap<String,Object>();
        queryParams.put("acctNum",id);
        queryParams.put("acctType","NA");
        queryParams.put("vendorId",vendorId);
        queryParams.put("acctName","john doe");
        given().
                spec(requestSpecVersion2).queryParameters(queryParams).
                when().
                post("account/"+organization).then().statusCode(200);
    }

    @Test(dataProvider="encrypt")
    public void getAccount(String id){
        Map<String,Object> queryParams = new HashMap<String,Object>();
        queryParams.put("acctNum",id);
        queryParams.put("acctType","NA");
        queryParams.put("vendorId",vendorId);
        Response response =given().
                spec(requestSpecVersion2).queryParameters(queryParams).
                when().
                get("account/"+organization).then().contentType(ContentType.JSON).extract().response();
        List<Map<String, Object>> accounts=response.jsonPath().getList("accounts");

        Object paperless = accounts.get(0).get("paperlessStatus");
        List notification =(List) accounts.get(0).get("notifyPref");
        System.out.println("account name = "+accounts.get(0).get("acctName"));
        System.out.println("account status = "+accounts.get(0).get("status"));
        ObjectMapper oMapper = new ObjectMapper();
        Map<String, Object> map;
        if(notification.size()<0){
            map = oMapper.convertValue(notification.get(0), Map.class);
            String typeValue;
            if(accounts.get(0).get("acctName") == "sms"){
                typeValue = (String)map.get("sms");
            }else{
                typeValue = (String)map.get("emailAddress");
            }
            System.out.println("notification Prefence = "+typeValue);
        }
        map = oMapper.convertValue(paperless, Map.class);
        System.out.println("Delivery Preference = "+map.get("newDeliveryPref"));
    }
}
