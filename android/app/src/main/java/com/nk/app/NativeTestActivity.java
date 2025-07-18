package com.nk.app;

import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.CreatePublicKeyCredentialResponse;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.CreateCredentialException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class NativeTestActivity extends AppCompatActivity {
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_test);

        Button btn = findViewById(R.id.button_test_passkey);
        output = findViewById(R.id.text_output);

        btn.setOnClickListener(v -> testPasskey());
    }

    private void testPasskey() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            output.setText("Passkeys not supported");
            return;
        }

        try {
            JSONObject json = new JSONObject();

            // Base64url challenge (без паддінгу)
            byte[] challengeBytes = "test-challenge".getBytes(StandardCharsets.UTF_8);
            String challengeBase64Url = Base64.encodeToString(challengeBytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            json.put("challenge", challengeBase64Url);

            // RP
            JSONObject rp = new JSONObject();
            rp.put("id", "example.com");
            rp.put("name", "Example");
            json.put("rp", rp);

            // User
            JSONObject user = new JSONObject();
            byte[] userIdBytes = "1".getBytes(StandardCharsets.UTF_8);
            String userIdBase64Url = Base64.encodeToString(userIdBytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            user.put("id", userIdBase64Url);
            user.put("name", "testuser");
            user.put("displayName", "Test User");
            json.put("user", user);

            // Алгоритм (ES256)
            JSONArray pubKeyCredParams = new JSONArray();
            JSONObject alg = new JSONObject();
            alg.put("type", "public-key");
            alg.put("alg", -7);
            pubKeyCredParams.put(alg);
            json.put("pubKeyCredParams", pubKeyCredParams);

            // Опціонально: timeout та attestation
            json.put("timeout", 60000);
            json.put("attestation", "none");

            // Створення запиту
            CreatePublicKeyCredentialRequest request = new CreatePublicKeyCredentialRequest(json.toString());

            CredentialManager credentialManager = CredentialManager.create(this);
            credentialManager.createCredentialAsync(
                    this,
                    request,
                    null,
                    getMainExecutor(),
                    new CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>() {
                        @Override
                        public void onResult(CreateCredentialResponse result) {
                            CreatePublicKeyCredentialResponse pk = (CreatePublicKeyCredentialResponse) result;
                            output.setText(pk.getRegistrationResponseJson());
                        }

                        @Override
                        public void onError(CreateCredentialException e) {
                            output.setText("Error: " + e.getMessage());
                        }
                    });

        } catch (Exception ex) {
            output.setText("Error: " + ex.getMessage());
        }
    }
}
