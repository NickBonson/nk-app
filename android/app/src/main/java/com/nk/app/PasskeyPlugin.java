package com.nk.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.Fido2ApiClient;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorResponse;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONObject;

@CapacitorPlugin(name = "Passkey")
public class PasskeyPlugin extends Plugin {
    private static final int REGISTER_REQUEST = 10001;
    private static final int SIGN_REQUEST = 10002;

    private PluginCall savedCall;

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

            PublicKeyCredentialCreationOptions options =
                    PublicKeyCredentialCreationOptions.fromJson(json.toString());

            Fido2ApiClient client = Fido.getFido2ApiClient(getContext());
            Task<PendingIntent> task = client.getRegisterPendingIntent(options);
            task.addOnSuccessListener(new OnSuccessListener<PendingIntent>() {
                @Override
                public void onSuccess(PendingIntent pendingIntent) {
                    savedCall = call;
                    startActivityForResult(call, pendingIntent, REGISTER_REQUEST);
                }
            });
            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
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

            PublicKeyCredentialRequestOptions options =
                    PublicKeyCredentialRequestOptions.fromJson(json.toString());

            Fido2ApiClient client = Fido.getFido2ApiClient(getContext());
            Task<PendingIntent> task = client.getSignPendingIntent(options);
            task.addOnSuccessListener(new OnSuccessListener<PendingIntent>() {
                @Override
                public void onSuccess(PendingIntent pendingIntent) {
                    savedCall = call;
                    startActivityForResult(call, pendingIntent, SIGN_REQUEST);
                }
            });
            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    call.reject(e.getMessage());
                }
            });
        } catch (Exception ex) {
            call.reject(ex.getMessage());
        }
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);
        if (savedCall == null) {
            return;
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            savedCall.reject("Canceled");
            savedCall = null;
            return;
        }

        try {
            AuthenticatorResponse response = Fido.getFido2ApiClient(getContext())
                    .getFido2PendingIntentResponse(data);
            JSObject ret = new JSObject();
            if (response instanceof AuthenticatorAttestationResponse) {
                ret.put("clientDataJSON", ((AuthenticatorAttestationResponse) response).getClientDataJSON());
                ret.put("attestationObject", ((AuthenticatorAttestationResponse) response).getAttestationObject());
            } else if (response instanceof AuthenticatorAssertionResponse) {
                ret.put("clientDataJSON", ((AuthenticatorAssertionResponse) response).getClientDataJSON());
                ret.put("authenticatorData", ((AuthenticatorAssertionResponse) response).getAuthenticatorData());
                ret.put("signature", ((AuthenticatorAssertionResponse) response).getSignature());
                ret.put("userHandle", ((AuthenticatorAssertionResponse) response).getUserHandle());
            }
            savedCall.resolve(ret);
        } catch (Exception e) {
            savedCall.reject(e.getMessage());
        }
        savedCall = null;
    }
}
