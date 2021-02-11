package com.example.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.TextView;

import java.util.ArrayList;

class MySpeechRecognizer implements RecognitionListener {
    private final static int ONE_RESULT = 1;
    private final SpeechRecognizer mSpeechRecognizer;
    private final Context mContext;
    private TextView mResultView;

    MySpeechRecognizer(Context context) {
        mContext = context;
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        mSpeechRecognizer.setRecognitionListener(this);
    }

    public void startListening(TextView result) {
        mResultView = result;
        if (SpeechRecognizer.isRecognitionAvailable(mContext)) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, mContext.getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, ONE_RESULT);
            mSpeechRecognizer.startListening(intent);
        } else {
            mResultView.setText("service not available?");
        }
    }

    public void destroy() {
        mSpeechRecognizer.destroy();
    }

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {
        mResultView.setText("error? " + error);
    }

    @Override
    public void onResults(Bundle results) {
        if (results != null) {
            ArrayList<String> spokenResults = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (spokenResults.size() > 0) {
                mResultView.setText(spokenResults.get(0));
            } else {
                mResultView.setText("no results?");
            }
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
}
