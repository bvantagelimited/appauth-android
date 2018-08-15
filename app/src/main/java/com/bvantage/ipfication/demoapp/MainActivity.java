package com.bvantage.ipfication.demoapp;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bvantage.ipfication.demoapp.network.Service;
import com.bvantage.ipfication.demoapp.request.OauthRequest;
import com.bvantage.ipfication.demoapp.response.OauthResponse;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String REALM_NAME = "mctest";
    private static final String CLIENT_ID = "appauth";
    private static final String AUTH_URL = "https://api.ipification.com/auth/realms/%s/protocol/openid-connect/auth";
    private static final String TOKEN_URL = "https://api.ipification.com/auth/realms/%s/protocol/openid-connect/token";

    private String loginHint;
    public static final int RC_AUTH = 1000;
    private AuthState mStateManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btIPLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginviaIPification();
            }
        });
    }


    private void loginviaIPification() {
        AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                Uri.parse(String.format(AUTH_URL, REALM_NAME)) /* auth endpoint */,
                Uri.parse(String.format(TOKEN_URL, REALM_NAME)) /* token endpoint */
        );
        AuthorizationService authorizationService = new AuthorizationService(this);
        Uri redirectUri = Uri.parse("com.ezyplanet.thousandhands.staging:/oauth2callback");
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                serviceConfiguration,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                redirectUri
        );
        builder.setScopes("openid email profile");

        if (getLoginHint() != null) {
            builder.setLoginHint(getLoginHint());
            Log.i(TAG, String.format("login_hint: %s", getLoginHint()));
        }

        AuthorizationRequest request = builder.build();
        CustomTabsIntent.Builder intentBuilder =
                authorizationService.createCustomTabsIntentBuilder(request.toUri());
        intentBuilder.setToolbarColor(getColorCompat(R.color.colorPrimary));
        Intent intent = authorizationService.getAuthorizationRequestIntent(
                request,
                intentBuilder.build());
        startActivityForResult(intent, RC_AUTH);

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == -1){
            // the stored AuthState is incomplete, so check if we are currently receiving the result of
            // the authorization flow from the browser.
            AuthorizationResponse response = AuthorizationResponse.fromIntent(data);
            AuthorizationException ex = AuthorizationException.fromIntent(data);
            mStateManager = new AuthState();
            if (response != null || ex != null) {
                mStateManager.update(response, ex);
            }
            if (response != null && response.authorizationCode != null) {
                // authorization code exchange is required
                mStateManager.update(response, ex);
                exchangeAuthorizationCode(response);
            } else if (ex != null) {
//                displayNotAuthorized("Authorization flow failed: " + ex.getMessage());
            } else {
//                displayNotAuthorized("No authorization state retained - reauthorization required");
            }

        }else{
        }
    }


    @MainThread
    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
//        displayLoading("Exchanging authorization code");
        performTokenRequest(
                authorizationResponse.createTokenExchangeRequest(),
                this::handleCodeExchangeResponse);
    }

    @MainThread
    private void performTokenRequest(
            TokenRequest request,
            AuthorizationService.TokenResponseCallback callback) {
        ClientAuthentication clientAuthentication;
        try {
            clientAuthentication = mStateManager.getClientAuthentication();
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            Log.d("aaa", "Token request cannot be made, client authentication for the token "
                    + "endpoint could not be constructed (%s)", ex);
//            displayNotAuthorized("Client authentication method is unsupported");
            return;
        }
        AuthorizationService authorizationService = new AuthorizationService(this);
        authorizationService.performTokenRequest(
                request,
                clientAuthentication,
                callback);
    }

    @WorkerThread
    private void handleCodeExchangeResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {

        mStateManager.update(tokenResponse, authException);
        if (!mStateManager.isAuthorized()) {
            final String message = "Authorization Code exchange failed"
                    + ((authException != null) ? authException.error : "");

            // WrongThread inference is incorrect for lambdas
            //noinspection WrongThread
//            runOnUiThread(() -> displayNotAuthorized(message));
        } else {
            doLoginIP(mStateManager);
        }
    }


    public void doLoginIP(AuthState authState) {
        OauthRequest oauthRequest = OauthRequest.IPNetWorkRequest(authState.getAccessToken(), REALM_NAME, getLoginHint());
        authorizeIPCallBack(oauthRequest);
    }


    private void authorizeIPCallBack(OauthRequest oauthRequest ){
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://st-api.thousandhands.com/").client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        Service service = retrofit.create(Service.class);
        service.generateAccessToken(oauthRequest).enqueue(new Callback<OauthResponse>() {
            @Override
            public void onResponse(Call<OauthResponse> call, Response<OauthResponse> response) {
                if(response.code() == 200 && response.body() != null){
                    Log.d("service","response " + response.body());
                    gotoUserActivity("TH");
                }else{
                    Toast.makeText(getApplicationContext(), "Error: "+ response.code() + response.errorBody(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<OauthResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.d("service","onFailure " + t.getMessage());
            }
        });

    }


    private void gotoUserActivity(String preferred_username) {
        Intent intent = new Intent(this, UserActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("preferred_username",preferred_username);
        intent.putExtras(bundle);
        startActivity(intent);
    }


    public String getLoginHint() {
        return loginHint;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation")
    private int getColorCompat(@ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(color);
        } else {
            return getResources().getColor(color);
        }
    }
}
