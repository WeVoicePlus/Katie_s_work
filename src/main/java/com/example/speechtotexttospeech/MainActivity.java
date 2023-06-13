package com.example.speechtotexttospeech;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;

import android.os.CountDownTimer;
import android.os.StrictMode;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.speech.tts.TextToSpeech;

import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import org.json.JSONException;

public class MainActivity extends AppCompatActivity {

    EditText txtView;
    TextView txtOutput;
    TextView txtStatus;
    Button btnSpeak;
    Button btnChat;
    TextToSpeech t1;
    String response;
    CountDownTimer countDownTimer;

    // microsoft
    // Configuration for speech recognition

    // Replace below with your own subscription key
    private static final String SpeechSubscriptionKey = "0e2eb3b3ad82457e9af6709646d4524d";
    // Replace below with your own service region (e.g., "westus").
    private static final String SpeechRegion = "eastasia";

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

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        txtView = findViewById(R.id.txtSpeak);
        txtOutput = findViewById(R.id.txtOutput);
        txtStatus = findViewById(R.id.txtSpeechStatus);
        txtOutput.setMovementMethod(new ScrollingMovementMethod()); // scrollable text box
        btnSpeak = findViewById(R.id.btnSpeech);

        // microsoft

        // Initialize SpeechSDK and request required permissions.
        try {
            // a unique number within the application to allow
            // correlating permission request responses with the request.
            int permissionRequestId = 5;

            // Request permissions needed for speech recognition
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, permissionRequestId);
        } catch(Exception ex) {
            Log.e("SpeechSDK", "could not init sdk, " + ex);
            txtOutput.setText("Could not initialize: " + ex);
        }

        // create config
        final SpeechConfig speechConfig;
        try {
            speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
            speechConfig.setSpeechRecognitionLanguage("yue-CN");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            txtView.setText(ex.getMessage() + System.lineSeparator() + TextUtils.join(System.lineSeparator(), ex.getStackTrace()));
            return;
        }

        t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!= TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.forLanguageTag("zh-yue"));
                    t1.setPitch(1);
                    t1.setSpeechRate(1);
                }
            }
        });


//     // button to chat
//     // on create
//            btnChat = findViewById(R.id.btnChat);
//            btnChat.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    try {
//                        chat();
//                    } catch (JSONException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            });

        // recognize
        btnSpeak.setOnClickListener(view -> {
            final String logTag = "reco 1";
            txtStatus.setText("Listening");

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
                setOnTaskCompletedListener(task, result -> {
                    txtStatus.setText("Not listening");
                    String s = result.getText();
                    if (result.getReason() != ResultReason.RecognizedSpeech) {
                        String errorDetails = (result.getReason() == ResultReason.Canceled) ? CancellationDetails.fromResult(result).getErrorDetails() : "";
                        s = "Recognition failed with " + result.getReason() + errorDetails;
                        s += System.lineSeparator() + "Speak again. Speak louder";

                        reco.close();
                        Log.i(logTag, "Recognizer returned: " + s);
                        txtOutput.setText(s);
                    } else {
                        reco.close();
                        Log.i(logTag, "Recognizer returned: " + s);
//                        setRecognizedText(s);
                        String txt = txtView.getText().toString();
                        txtView.setText(txt+System.lineSeparator() + s);

                        try {
                            chat();
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

//                        countDownTimer = new CountDownTimer(1000, 1000) {
//                            public void onFinish() {
//                                // When timer is finished
//                                // Execute your code here
//                                try {
//                                    chat();
//                                } catch (JSONException e) {
//                                    throw new RuntimeException(e);
//                                }
//                            }
//
//                            public void onTick(long millisUntilFinished) {
//                                // millisUntilFinished    The amount of time until finished.
//                            }
//                        }.start();
                    }

                });
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                txtView.setText(ex.getMessage() + System.lineSeparator() + TextUtils.join(System.lineSeparator(), ex.getStackTrace()));
            }
        });

    }

    //openai api (implementation 'com.theokanning.openai-gpt3-java:service:0.12.0')
    private void chat() throws JSONException, InterruptedException {
        if(txtView.getText().toString().length()==0) {
            txtOutput.setText("Please enter something");
        } else {
//            btnChat.setVisibility(View.GONE);
//            new CountDownTimer(5000, 1000) {
//                public void onFinish() {
//                    // When timer is finished
//                    btnChat.setVisibility(View.VISIBLE);
//                }
//
//                public void onTick(long millisUntilFinished) {
//                    // millisUntilFinished    The amount of time until finished.
//                }
//            }.start();

            OpenAiService service = new OpenAiService("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");
            ChatMessage message = new ChatMessage("user", txtView.getText().toString());
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(message);
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                    .messages(messages)
                    .model("gpt-3.5-turbo")
                    .maxTokens(20) //TODO: more tokens required to get complete sentences
                    .build();
//            Log.e(TAG, "input: "+chatCompletionRequest.toString());

    //        ChatCompletionResult postresponse = service.createChatCompletion(chatCompletionRequest);
    //        txtView.setText(postresponse.toString());

            List<ChatCompletionChoice> postresponse = service.createChatCompletion(chatCompletionRequest).getChoices();
            response = postresponse.get(0).getMessage().getContent();
            txtOutput.setText(response);
        }

        String txt = txtOutput.getText().toString();
        t1.speak(txt, TextToSpeech.QUEUE_FLUSH, null);
        txtView.setText("");

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

    private static ExecutorService  s_executorService;
    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    private void setRecognizedText(final String s) {
        AppendTextLine(s, true);
    }

    private void AppendTextLine(final String s, final Boolean erase) {
        MainActivity.this.runOnUiThread(() -> {
            if (erase) {
                txtView.setText(s);
            } else {
                String txt = txtView.getText().toString();
                txtView.setText(txt + System.lineSeparator() + s);
            }
        });
    }

}

// //Speech to text with build in google
// // on create
//        btnSpeak.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                SpeakNow(v);
//            }
//        });

//    private void SpeakNow(View view) {
//        if(countDownTimer!=null)countDownTimer.cancel();
//        try {
//            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5);
//            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
////        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
//            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening...");
//            startActivityForResult(intent, 0);
//        } catch (ActivityNotFoundException e) {
//            String appPackageName = "com.google.android.googlequicksearchbox";
//            try {
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
//            } catch (android.content.ActivityNotFoundException anfe) {
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
//            }
//
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == 0 && resultCode == RESULT_OK) {
//            String text = txtView.getText().toString();
//            text += data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
//            txtView.setText(text);
//        }
//        countDownTimer = new CountDownTimer(1000, 1000) {
//            public void onFinish() {
//                // When timer is finished
//                // Execute your code here
//                try {
//                    chat();
//                } catch (JSONException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//            public void onTick(long millisUntilFinished) {
//                // millisUntilFinished    The amount of time until finished.
//            }
//        }.start();
//    }
