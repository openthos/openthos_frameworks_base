package com.emindsoft.setupwizard;

import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.SystemProperties;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.content.Context;

import java.io.IOException;
import java.io.DataOutputStream;

public class UserSetupActivity extends BaseActivity {
    private Button mButtonFinish;
    private Button mButtonPrev;
    private EditText mEditTextUsername;
    private EditText mComputername;
    private EditText mOldPassword;
    private EditText mNewPassword;
    private String screenPassword;
    private DevicePolicyManager devicePolicyManager;
    private TextView mSkip;
    private String defaultComputerName;
    private String computerName;

    private final Runnable mRequestFocus = new Runnable() {
        public void run() {
            if (UserSetupActivity.this.mButtonFinish != null && UserSetupActivity.this.mButtonFinish.isEnabled()) {
                UserSetupActivity.this.mButtonFinish.requestFocusFromTouch();
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setup);
        upgradeRootPermission(getPackageCodePath());
        this.mButtonPrev = (Button) findViewById(R.id.button_prev);
        this.mButtonFinish = (Button) findViewById(R.id.button_finish);
        this.mEditTextUsername = (EditText) findViewById(R.id.edittext_username);
        this.mComputername = (EditText) findViewById(R.id.edittext_computer_name);
        this.mOldPassword = (EditText) findViewById(R.id.edittext_screen_password);
        this.mNewPassword = (EditText) findViewById(R.id.edittext_screen_password_confirm);
        this.mSkip = (TextView) findViewById(R.id.text_skip);

        defaultComputerName = SystemProperties.get("ro.build.host");
        this.mComputername.setText(defaultComputerName);
        String userName = UserManager.get(this).getUserName();
        if (!TextUtils.equals(userName, "\u673a\u4e3b")) {
            this.mEditTextUsername.setText(userName);
        }
        computerName = this.mComputername.getText().toString().trim();
        final String oldPassword = this.mOldPassword.getText().toString().trim();
        final String newPassword = this.mNewPassword.getText().toString().trim();
        devicePolicyManager = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);

        this.mButtonFinish.setEnabled(!TextUtils.isEmpty(this.mEditTextUsername.getText().toString().trim()));
        this.mEditTextUsername.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != 0 && actionId != 6) {
                    return false;
                }
                ((InputMethodManager) UserSetupActivity.this.getSystemService("input_method")).hideSoftInputFromWindow(v.getWindowToken(), 0);
                String oldUserName = UserManager.get(UserSetupActivity.this).getUserName();
                String newUserName = UserSetupActivity.this.mEditTextUsername.getText().toString();
                if (!TextUtils.isEmpty(newUserName)) {
                    if (!newUserName.equals(oldUserName)) {
                        UserManager.get(UserSetupActivity.this).setUserName(UserHandle.myUserId(), newUserName);
                    }
                    ((SetupWizardApplication) UserSetupActivity.this.getApplication()).onSetupFinished(UserSetupActivity.this);
                }
                return true;
            }
        });
        this.mEditTextUsername.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
                UserSetupActivity.this.mButtonFinish.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }
        });
        this.mButtonFinish.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //save computer name
                //FIXME: it did not work well
                SystemProperties.set("ro.build.host",computerName);
                String oldUserName = UserManager.get(UserSetupActivity.this).getUserName();
                String newUserName = UserSetupActivity.this.mEditTextUsername.getText().toString();
                if (!(TextUtils.isEmpty(newUserName) || newUserName.equals(oldUserName))) {
                    UserManager.get(UserSetupActivity.this).setUserName(UserHandle.myUserId(), newUserName);
                }
                if (!TextUtils.isEmpty(oldPassword) && !TextUtils.isEmpty(newPassword) && oldPassword.equals(newPassword)) {
                    //reset password
                    //FIXME: it did not work well
                    devicePolicyManager.resetPassword(oldPassword, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
                }
                else{
                    Toast.makeText(UserSetupActivity.this, "you have null input or the twice password was not the same", Toast.LENGTH_SHORT).show();
                }
                ((SetupWizardApplication) UserSetupActivity.this.getApplication()).onSetupFinished(UserSetupActivity.this);
            }
        });
        this.mButtonPrev.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                UserSetupActivity.this.onBackPressed();
            }
        });
        this.mSkip.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ((SetupWizardApplication) UserSetupActivity.this.getApplication()).onSetupFinished(UserSetupActivity.this);
            }
        });
    }

    public void onResume() {
        super.onResume();
        this.mRequestFocus.run();
        new Handler().postDelayed(this.mRequestFocus, 500);
    }

    //get the permission of root
    public static boolean upgradeRootPermission(String pkgCodePath) {
        Process process = null;
        DataOutputStream os = null;
        try {
            String cmd="chmod 777 " + pkgCodePath;
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
        return true;
    }
}
