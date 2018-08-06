package vstore.framework.rule;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

public class VStoreRuleTests {


    @Test
    public void ruleCreationFromJsonTest() {
        String ruleJson = "{\"uuid\" : \"test_rule_1\"," +
                "            \"name\" : \"Test Rule\"," +
                "            \"dateCreation\" : 1531152679," +
                "            \"mimetypes\" : [\"image/jpeg\", \"image/png\", \"image/gif\"]," +
                "            \"sharingDomain\" : 0," +
                "            \"weekdays\" : \"1,2,3,5,6,7\"," +
                "            \"timeStart\" : \"10:00\"," +
                "            \"timeEnd\" : \"22:00\"," +
                "            \"context\" :" +
                "            {" +
                "                \"location\" : { \"lat\" : 50.2, \"lng\" : 3.6 }," +
                "                \"radius\" : 300" +
                "            }," +
                "            \"scoring\" :" +
                "            {" +
                "                \"s_location\" : 5.0" +
                "                \"s_weekdays\" : 3.0" +
                "            }," +
                "            \"decisions\" : [" +
                "                {" +
                "                    \"isSpecific\" : false," +
                "                    \"selectedType\" : \"CLOUDLET\"," +
                "                    \"maxRadius\" : 30" +
                "                }," +
                "                {" +
                "                    \"isSpecific\" : false," +
                "                    \"selectedType\" : \"CLOUDLET\"," +
                "                    \"maxRadius\" : 30" +
                "                }" +
                "            ]," +
                "            \"isGlobal\" : true," +
                "            \"storeMultiple\" : true" +
                "        }";

        //Create rule from json
        JSONParser jParser = new JSONParser();
        JSONObject j;
        try
        {
            j = (JSONObject) jParser.parse(ruleJson);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            //TODO Exit test with error
            return;
        }
        VStoreRule rule = new VStoreRule(j);

        Assert.assertTrue(rule.hasLocationContext());
        //Assert.assertTrue(rule.getDetailScore() == ((float)8.0));
    }
}
