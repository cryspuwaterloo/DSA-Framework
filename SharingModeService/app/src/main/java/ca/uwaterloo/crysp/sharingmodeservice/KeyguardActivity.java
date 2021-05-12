package ca.uwaterloo.crysp.sharingmodeservice;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import ca.uwaterloo.crysp.sharingmodeservice.services.SharingModeService;

public class KeyguardActivity extends Activity {
    private static final String TAG = "KeyGuardActivity";
    private static final int LOCK_REQUEST_CODE = 100;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("KEY", "Here");
        showAuthentication();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    private void showAuthentication() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        Intent screenLockIntent = keyguardManager.createConfirmDeviceCredentialIntent(
                "Device return authentication", "authenticate the current user"
        );
        startActivityForResult(screenLockIntent, LOCK_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOCK_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // let sharing mode service know its ok
                Log.i("Authentication", "successful");
                Intent intent = new Intent(this, SharingModeService.class);
                intent.setAction(SharingModeService.RESULT_AUTHENTICATION_SUCCESS);
                startService(intent);
            } else {
                // let sharing mode service know its not ok
                Log.d(TAG, "Not good");
                Intent intent = new Intent(this, SharingModeService.class);
                intent.setAction(SharingModeService.RESULT_AUTHENTICATION_FAILURE);
                startService(intent);
            }
        }
        finish();
    }

    @Override
    protected void onUserLeaveHint()
    {
        Log.d("onUserLeaveHint","Home button pressed");
        //Intent intent = new Intent(this, SharingModeService.class);
        //intent.setAction(SharingModeService.ACTION_CAPTURE_KEYGUARD_ESCAPER);
        //startService(intent);
        super.onUserLeaveHint();
    }

    @Override
    protected void onPause() {
        Log.d("onPause","Home button pressed");
        // Intent intent = new Intent(this, SharingModeService.class);
        // intent.setAction(SharingModeService.ACTION_CAPTURE_KEYGUARD_ESCAPER);
        // startService(intent);
        super.onPause();
    }
}
