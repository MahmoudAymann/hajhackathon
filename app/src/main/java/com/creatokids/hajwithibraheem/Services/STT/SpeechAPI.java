package com.creatokids.hajwithibraheem.Services.STT;

/**
 * Created by Amr Winter on 20/01/2018.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.creatokids.hajwithibraheem.R;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;

import static com.creatokids.hajwithibraheem.Global.MethodFactory.logMessage;
import static com.creatokids.hajwithibraheem.Global.MethodFactory.sendBroadCast;


public class SpeechAPI implements Serializable {

    public static final List<String> SCOPE = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    public static final String TAG = "SpeechAPI";
    public static final String SPECCH_API_INTENT = "SpeechAPI";



    public boolean isAPIComplete = false;

    private static final String PREFS = "SpeechService";
    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";

    /**
     * We reuse an access token if its expiration time is longer than this.
     */
    private static final int ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000; // thirty minutes

    /**
     * We refresh the current access token before it expires.
     */
    private static final int ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000; // one minute

    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;
    @Nullable
    private static Handler mHandler;

    //private final SpeechBinder mBinder = new SpeechBinder();
    private final ArrayList<Listener> mListeners = new ArrayList<>();

    @Nullable
    private final StreamObserver<StreamingRecognizeResponse> mResponseObserver = new StreamObserver<StreamingRecognizeResponse>() {
        @Override
        public void onNext(StreamingRecognizeResponse response) {
            String text = null;
            boolean isFinal = false;
            if (response.getResultsCount() > 0) {
                final StreamingRecognitionResult result = response.getResults(0);
                isFinal = result.getIsFinal();
                if (result.getAlternativesCount() > 0) {
                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                    text = alternative.getTranscript();
                }
            }
            if (text != null) {
                for (Listener listener : mListeners) {
                    listener.onSpeechRecognized(text, isFinal);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            logMessage(TAG, "API not completed.");
            logMessage(TAG, "Error calling the API." + t.getLocalizedMessage());
            Intent iApiComplete = new Intent(SPECCH_API_INTENT);
            iApiComplete.putExtra("apiStatus", false);
//            sendBroadCast(mContext, iApiComplete, "SpeechAPI: API not Complete");
        }

        @Override
        public void onCompleted() {
            logMessage(TAG, "API completed.");

//            Intent iApiComplete = new Intent(SPECCH_API_INTENT);
//            iApiComplete.putExtra("apiStatus", false);
//            sendBroadCast(mContext, iApiComplete, "SpeechAPI: API Complete");

        }

    };
    private Context mContext;
    @Nullable
    private volatile AccessTokenTask mAccessTokenTask;
    private final Runnable mFetchAccessTokenRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAccessToken();
        }
    };
    @Nullable
    private SpeechGrpc.SpeechStub mApi;
    @Nullable
    private StreamObserver<StreamingRecognizeRequest> mRequestObserver;

    private boolean instantiatedSpeechAPI = false;

    public boolean isAPIComplete() {
        return isAPIComplete;
    }

    public SpeechAPI(Context mContext) {
        this.mContext = mContext;
        mHandler = new Handler();
        fetchAccessToken();
    }

    public boolean isInstantiatedSpeechAPI() {
        return instantiatedSpeechAPI;
    }

    public void destroy() {
        if (mHandler != null){
            mHandler.removeCallbacks(mFetchAccessTokenRunnable);
            mHandler = null;
        }
        // Release the gRPC channel.
        if (mApi != null) {
            final ManagedChannel channel = (ManagedChannel) mApi.getChannel();
            if (channel != null && !channel.isShutdown()) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logMessage(TAG, "Error shutting down the gRPC channel."+ e.getLocalizedMessage());
                }
            }
            mApi = null;
        }
    }

    private void fetchAccessToken() {
        if (mAccessTokenTask != null) {
            return;
        }
        mAccessTokenTask = new AccessTokenTask();
        mAccessTokenTask.execute();
    }

    public void addListener(@NonNull Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Starts recognizing speech audio.
     *
     * @param sampleRate The sample rate of the audio.
     */
    public void startRecognizing(int sampleRate) {
        if (mApi == null) {
            logMessage(TAG, "API not ready. Ignoring the request.");
            return;
        }

        // Configure the API
        mRequestObserver = mApi.streamingRecognize(mResponseObserver);

        StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
                .setConfig(RecognitionConfig.newBuilder()
//                        .setLanguageCode("en-US")
                        .setLanguageCode("ar-EG")
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(sampleRate)
                        .build()
                )
                .setInterimResults(true)
                .setSingleUtterance(true)
                .build();

        StreamingRecognizeRequest streamingRecognizeRequest = StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build();

        logMessage(TAG, "Recognizing Started");
        isAPIComplete = true;
        Intent iApiComplete = new Intent(SPECCH_API_INTENT);
        iApiComplete.putExtra("apiStatus", true);
        sendBroadCast(mContext, iApiComplete, "startRecognizing(): Recognizing Started");

        if (mRequestObserver != null) mRequestObserver.onNext(streamingRecognizeRequest);

    }

    /**
     * Recognizes the speech audio. This method should be called every time a chunk of byte buffer
     * is ready.
     *
     * @param data The audio data.
     * @param size The number of elements that are actually relevant in the {@code data}.
     */
    public void recognize(byte[] data, int size) {
        if (mRequestObserver == null) {
            return;
        }
        try {
            // Call the streaming recognition API
            mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(data, 0, size))
                    .build());
        }catch (Exception e){
            logMessage("stt", e.getMessage());
        }
        // TODO: 21/03/2018 fixing stt memory
        data = null;
    }

    /**
     * Finishes recognizing speech audio.
     */
    public void finishRecognizing() {
        if (mRequestObserver == null) {
            return;
        }
        mRequestObserver.onCompleted();
        mRequestObserver = null;
    }

    public interface Listener {
        //Called when a new piece of text was recognized by the Speech API.
        void onSpeechRecognized(String text, boolean isFinal);
    }

    private class AccessTokenTask extends AsyncTask<Void, Void, AccessToken> {

        @Nullable
        @Override
        protected AccessToken doInBackground(Void... voids) {

            final SharedPreferences prefs = mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String tokenValue = prefs.getString(PREF_ACCESS_TOKEN_VALUE, null);
            long expirationTime = prefs.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1);

            // Check if the current token is still valid for a while = 30 minute
            if (tokenValue != null && expirationTime > 0) {
                if (expirationTime > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TOLERANCE) {
                    // set the flag of SpeechAPI instantiation equals true
                    instantiatedSpeechAPI = true;
                    return new AccessToken(tokenValue, new Date(expirationTime));
                }
            }

            final InputStream stream = mContext.getResources().openRawResource(R.raw.learn_with_jack_stt);
            try {
                final GoogleCredentials credentials = GoogleCredentials.fromStream(stream).createScoped(SCOPE);
                final AccessToken token = credentials.refreshAccessToken();
                prefs.edit()
                        .putString(PREF_ACCESS_TOKEN_VALUE, token.getTokenValue())
                        .putLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, token.getExpirationTime().getTime())
                        .apply();
                // set the flag of SpeechAPI instantiation equals true
                instantiatedSpeechAPI = true;
                return token;
            } catch (IOException e) {
                logMessage(TAG, "Failed to obtain access token." + e.getLocalizedMessage());
                instantiatedSpeechAPI = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(@Nullable AccessToken accessToken) {
            mAccessTokenTask = null;
            // if the time or date is not correct the accessToken will be null.
            if (accessToken != null){

                final ManagedChannel channel = new OkHttpChannelProvider()
                        .builderForAddress(HOSTNAME, PORT)
                        .nameResolverFactory(new DnsNameResolverProvider())
                        .intercept(new GoogleCredentialsInterceptor(new GoogleCredentials(accessToken)
                                .createScoped(SCOPE)))
                        .build();
                mApi = SpeechGrpc.newStub(channel);

                // Schedule access token refresh before it expires
                if (mHandler != null) {
                    mHandler.postDelayed(mFetchAccessTokenRunnable,
                            Math.max(accessToken.getExpirationTime().getTime() - System.currentTimeMillis() - ACCESS_TOKEN_FETCH_MARGIN, ACCESS_TOKEN_EXPIRATION_TOLERANCE));
                }
            }else {
                instantiatedSpeechAPI = false;
            }
        }
    }
}