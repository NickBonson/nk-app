package com.nk.app;

import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.CreatePublicKeyCredentialResponse;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.CreateCredentialException;
import org.json.JSONObject;

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
            json.put("challenge", "test-challenge");
            JSONObject rp = new JSONObject();
            rp.put("id", "example.com");
            rp.put("name", "Example");
            json.put("rp", rp);
            JSONObject user = new JSONObject();
            user.put("id", "1");
            user.put("name", "Test User");
            json.put("user", user);

            CreatePublicKeyCredentialRequest request =
                    new CreatePublicKeyCredentialRequest(json.toString());

            CredentialManager credentialManager = CredentialManager.create(this);
            credentialManager.createCredentialAsync(
                    this,
                    request,
                    null,
                    this.getMainExecutor(),
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
