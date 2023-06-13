package com.example.translationapp;

import static android.content.ContentValues.TAG;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

import javax.xml.transform.Result;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Translator extends AsyncTask { // Translate the text to Chinese
    private static final String key = "b036a18e2c4c489b84fae6d4865c9b49"; // API_Key
    private static final String endpoint = "https://api.cognitive.microsofttranslator.com"; // Endpoint of Microsoft Translator API
    private static final String URL = endpoint + "/translate?api-version=3.0&from=%s&to=zh-tw";
    private MediaType JSON = MediaType.parse("application/json");

    private OkHttpClient client;
    private Gson gson;

    public Translator() {
        client = new OkHttpClient();
        gson = new Gson();
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        String result = "null";
        String text = handleSpecialSymbol(objects[0].toString());
        String language = objects[1].toString();
        String url = String.format(URL, language);
        String test = "[{\"Text\":'"+text+"'}]";
       try{
           RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), test);
           Request apiRequest = new Request.Builder()
                .url(url)
                .addHeader("Ocp-Apim-Subscription-Key", key)
                .addHeader("Ocp-Apim-Subscription-Region", "eastasia")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
           Response response = client.newCall(apiRequest).execute();

           result = response.body().string();
           JSONArray jsonArray = new JSONArray(result);
//           jsonArray: [{"translations":[{"text":"你好","to":"zh-Hant"}]}]
           JSONObject jObject  = jsonArray.getJSONObject(0);
//           jObject: {"translations":[{"text":"你好","to":"zh-Hant"}]}
           jsonArray = (JSONArray) jObject.get("translations");
//           jsonArray: [{"text":"你好","to":"zh-Hant"}]
           jObject = jsonArray.getJSONObject(0);
//           jObject: {"text":"你好","to":"zh-Hant"}
           result = jObject.get("text").toString();

        }
        catch (IOException e){
            result = e.toString();
       } catch (JSONException e) {
           throw new RuntimeException(e);
       }

        return result;
    }

    public String TranslateText(String text, String language)  {
        String[] value = {text, language};
        return doInBackground(value).toString();
    }

    public static String prettify(String json_text) {
        JsonParser parser = new JsonParser();
        JsonElement json = parser.parse(json_text);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);
    }

    public String handleSpecialSymbol(String text){
        char[] input = text.toCharArray();
        int specialSymbol = 0;
        char[] output = Arrays.copyOf(input, input.length);
        for(int i = 0; i<input.length;i++){
            if(input[i]=='\''){
                output = Arrays.copyOf(output, output.length+1);
                output[i]='\\';
                specialSymbol++;
            }
            output[i+specialSymbol] = input[i];
        }
        return new String(output);
    }

}
