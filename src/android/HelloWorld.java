package io.electrosoft.helloworld;

import android.widget.Toast;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

public class HelloWorld extends CordovaPlugin {
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(action.equals("nativeToast")){
            nativeToast();;
        }
        return false;
    }

    public void nativeToast(){
        Toast.makeText(webView.getContext(), "The Fucking Wrapper Works For Resolvea!", Toast.LENGTH_SHORT).show();
    }
}
