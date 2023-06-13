package com.example.translationapp;


import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    // Link to Layout
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> adapterLanguage;
    private Button translateBtn;
    private Button speechBtn;
    private EditText input;
    private TextView result;

    // Variable
    private String selectedLanguage;
    private String[] language = {"English", "Japanese", "French"};
    private String[] languageCode = {"en", "ja", "fr"};
    private String[] translationCode = {"en-GB", "ja-JP", "fr-FR"};

    // Configuration for speech recognition

    // Replace below with your own subscription key
    private static final String[] SpeechSubscriptionKey = {"0e2eb3b3ad82457e9af6709646d4524d", "786b9eeecf34499f8cb1a0a35764b057","4fd643fa8fec4cd580dd40db93d11eab"};

    // Replace below with your own service region (e.g., "westus").
    private static final String[] SpeechRegion = {"eastasia", "japaneast", "francecentral"};

    private MicrophoneStream microphoneStream;
    private MicrophoneStream createMicrophoneStream() {
        this.releaseMicrophoneStream();

        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }
    private void releaseMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        autoCompleteTextView = findViewById(R.id.auto_complete_txt);
//        translateBtn = findViewById(R.id.btn);
        speechBtn = findViewById(R.id.btn2);
        input = findViewById(R.id.input);
        result = findViewById(R.id.result);

        // Initialize SpeechSDK and request required permissions.
        try {
            // a unique number within the application to allow
            // correlating permission request responses with the request.
            int permissionRequestId = 5;

            // Request permissions needed for speech recognition
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, permissionRequestId);
        } catch(Exception ex) {
            Log.e("SpeechSDK", "could not init sdk, " + ex);
            result.setText("Could not initialize: " + ex);
        }

        // adapter for dropdown list
        adapterLanguage = new ArrayAdapter<>(this, R.layout.list_item, language);
        autoCompleteTextView.setAdapter(adapterLanguage);

        // Dropdown list listener
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();
                selectedLanguage = item;
            }
        });

//        //Translate Button Listener
//        translateBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                translate();
//            }
//        });

        speechBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // create config
                final SpeechConfig speechConfig;
                try {
                    int index = 0;
                    for(int i =0; i<language.length;i++){
                        if(selectedLanguage == language[i]){
                            index = i;
                        }
                    }
                    Log.i("key", String.valueOf(index));
                    speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey[index], SpeechRegion[index]);
                    speechConfig.setSpeechRecognitionLanguage(translationCode[index]);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    input.setText(ex.getMessage() + System.lineSeparator() + TextUtils.join(System.lineSeparator(), ex.getStackTrace()));
                    return;
                }

                final String logTag = "reco 1";
                result.setText("Listening");

                try {
                    // In general, if the device default microphone is used then it is enough
                    // to either have AudioConfig.fromDefaultMicrophoneInput or omit the audio
                    // config altogether.
                    // AudioConfig.fromStreamInput is specifically needed if you want to use an
                    // external microphone (including Bluetooth that couldn't be otherwise used)
                    // or mix audio from some other source to microphone audio.
                    final AudioConfig audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
                    final SpeechRecognizer reco = new SpeechRecognizer(speechConfig, audioInput);

                    final Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();
                    setOnTaskCompletedListener(task, resultT -> {
                        String s = resultT.getText();
                        if (resultT.getReason() != ResultReason.RecognizedSpeech) {
                            s = "Speak again. Speak louder";

                            reco.close();
                            Log.i(logTag, "Recognizer returned: " + s);
                            result.setText(s);
                        } else {
                            reco.close();
                            Log.i(logTag, "Recognizer returned: " + s);
                            input.setText(System.lineSeparator() + s);
                            translate();
                        }

                    });
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    input.setText(ex.getMessage() + System.lineSeparator() + TextUtils.join(System.lineSeparator(), ex.getStackTrace()));
                }

            }
        });

    }

    private void translate() {
        Log.i("translate", "translating");
        // reset the result text
        result.setText("");

        // check if the input is empty or not
        if(input.getText().toString().isEmpty()){
            Toast.makeText(MainActivity.this, "Please enter your text to translate", Toast.LENGTH_SHORT).show();
        }
        else {
            new Thread(networkTask).start();
        }
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    private static ExecutorService s_executorService;
    static {
            s_executorService = Executors.newCachedThreadPool();
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String value = data.getString("output");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    result.setText(value);

                }
            });
        }
    };

    Runnable networkTask = new Runnable() {
        @Override
        public void run() {
            //Message
            Message msg = new Message();
            Bundle data = new Bundle();

            String text = input.getText().toString();
            int index = 0;
            for(int i =0; i<language.length;i++){
                if(selectedLanguage == language[i]){
                    index = i;
                }
            }
            Log.i("selectedLanguage", String.valueOf(index));
            //Create new Translator
                Translator translator = new Translator();
                String output = translator.TranslateText(text, languageCode[index]);

                data.putString("output",output);
                msg.setData(data);
                handler.handleMessage(msg);

        }
    };

}

