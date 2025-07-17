package com.nk.app;

import android.os.Build;
import android.os.Looper;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPublicKeyCredentialOption;
import java.lang.reflect.Method;
import androidx.core.os.HandlerCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.concurrent.Executor;
import org.json.JSONObject;

@CapacitorPlugin(name = "Passkey")
public class PasskeyPlugin extends Plugin {
    private CredentialManager credentialManager;
    private final Executor executor = HandlerCompat.createAsync(Looper.getMainLooper())::post;

    @Override
    public void load() {
        super.load();
        credentialManager = CredentialManager.create(getContext());
    }

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
            JSONObject pubKey = new JSONObject();
            pubKey.put("challenge", challenge);
            JSONObject rp = new JSONObject();
            rp.put("id", rpId);
            rp.put("name", rpName);
            pubKey.put("rp", rp);
            JSONObject user = new JSONObject();
            user.put("id", userId);
            user.put("name", userName);
            pubKey.put("user", user);

            CreatePublicKeyCredentialRequest request = new CreatePublicKeyCredentialRequest(pubKey.toString());
            credentialManager.createCredentialAsync(getActivity(), request, null, executor,
                new CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>(call));
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
            JSONObject optionJson = new JSONObject();
            optionJson.put("challenge", challenge);
            optionJson.put("rpId", rpId);
            GetPublicKeyCredentialOption option = new GetPublicKeyCredentialOption(optionJson.toString());
            GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

            credentialManager.getCredentialAsync(getActivity(), request, null, executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>(call));
        } catch (Exception ex) {
            call.reject(ex.getMessage());
        }
    }

    private static class CredentialManagerCallback<R, E extends Exception> implements androidx.credentials.CredentialManagerCallback<R, E> {
        private final PluginCall call;
        CredentialManagerCallback(PluginCall call) {
            this.call = call;
        }

        @Override
        public void onResult(R response) {
            if (response instanceof CreateCredentialResponse) {
                Credential credential = extractCredential((CreateCredentialResponse) response);
                if (credential != null) {
                    handleCredential(credential);
                } else {
                    call.reject("Unable to extract credential");
                }
            } else if (response instanceof GetCredentialResponse) {
                handleCredential(((GetCredentialResponse) response).getCredential());
            } else {
                call.reject("Unknown response");
            }
        }

        private Credential extractCredential(CreateCredentialResponse response) {
            try {
                Method m = response.getClass().getMethod("getCredential");
                Object obj = m.invoke(response);
                if (obj instanceof Credential) {
                    return (Credential) obj;
                }
            } catch (Exception ignored) {
                // The method may not exist on older library versions
            }
            return null;
        }

        private void handleCredential(Credential credential) {
            JSObject ret = new JSObject();

            // getId() was introduced in newer versions of the Credentials
            // library. Use reflection so older versions can still compile.
            try {
                Method m = credential.getClass().getMethod("getId");
                Object obj = m.invoke(credential);
                if (obj instanceof CharSequence) {
                    ret.put("id", obj.toString());
                }
            } catch (Exception ignored) {
                // Ignore if getId() isn't available
            }

            ret.put("data", credential.getData().toString());
            call.resolve(ret);
        }

        @Override
        public void onError(E e) {
            call.reject(e.toString());
        }
    }
}
