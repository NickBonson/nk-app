package com.nk.app;

import android.os.Build;

import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePublicKeyCredentialResponse;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONObject;

@CapacitorPlugin(name = "Passkey")
public class PasskeyPlugin extends Plugin {

    @PluginMethod
    public void registerWithPasskey(PluginCall call) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            call.reject("Passkeys not supported");
            return;
        }

        String challenge = call.getString("challenge");
        String userId = call.getString("userId");
        String userName = call.getString("userName");
        String rpId = call.getString("rpId");
        String rpName = call.getString("rpName", rpId);

        if (challenge == null || userId == null || userName == null || rpId == null) {
            call.reject("Missing parameters");
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("challenge", challenge);
            JSONObject rp = new JSONObject();
            rp.put("id", rpId);
            rp.put("name", rpName);
            json.put("rp", rp);
            JSONObject user = new JSONObject();
            user.put("id", userId);
            user.put("name", userName);
            json.put("user", user);

            CreatePublicKeyCredentialRequest request =
                    new CreatePublicKeyCredentialRequest(json.toString());

            CredentialManager credentialManager = CredentialManager.create(getContext());
            credentialManager.createCredentialAsync(
                    getActivity(),
                    request,
                    null,
                    getActivity().getMainExecutor(),
                    new CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>() {
                        @Override
                        public void onResult(CreateCredentialResponse result) {
                            CreatePublicKeyCredentialResponse pk = (CreatePublicKeyCredentialResponse) result;
                            JSObject ret = new JSObject();
                            ret.put("responseJson", pk.getRegistrationResponseJson());
                            call.resolve(ret);
                        }

                        @Override
                        public void onError(CreateCredentialException e) {
                            call.reject(e.getMessage());
                        }
                    });
        } catch (Exception ex) {
            call.reject(ex.getMessage());
        }
    }

    @PluginMethod
    public void loginWithPasskey(PluginCall call) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            call.reject("Passkeys not supported");
            return;
        }

        String challenge = call.getString("challenge");
        String rpId = call.getString("rpId");
        if (challenge == null || rpId == null) {
            call.reject("Missing parameters");
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("challenge", challenge);
            json.put("rpId", rpId);

            GetCredentialRequest request = new GetCredentialRequest(
                    java.util.Collections.singletonList(
                            new GetPublicKeyCredentialOption(json.toString())));

            CredentialManager credentialManager = CredentialManager.create(getContext());
            credentialManager.getCredentialAsync(
                    getActivity(),
                    request,
                    null,
                    getActivity().getMainExecutor(),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            PublicKeyCredential pk = (PublicKeyCredential) result.getCredential();
                            JSObject ret = new JSObject();
                            ret.put("responseJson", pk.getAuthenticationResponseJson());
                            call.resolve(ret);
                        }

                        @Override
                        public void onError(GetCredentialException e) {
                            call.reject(e.getMessage());
                        }
                    });
        } catch (Exception ex) {
            call.reject(ex.getMessage());
        }
    }

    // No onActivityResult handling is required when using Credential Manager
}
