package com.bartoszlewandowski.uberclone;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.bartoszlewandowski.uberclone.driver.DriverRequestListActivity;
import com.bartoszlewandowski.uberclone.passenger.PassengerActivity;
import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;
import com.shashank.sony.fancytoastlib.FancyToast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    enum State {
        SIGNUP, LOGIN
    }

    @BindView(R.id.edtUsername)
    EditText edtUsername;
    @BindView(R.id.edtPassword)
    EditText edtPassword;
    @BindView(R.id.edtDriverOrPassenger)
    EditText edtDriverOrPassenger;
    @BindView(R.id.btnLogInOrSignUp)
    Button btnSignUpOrLogIn;
    @BindView(R.id.radioBtnDriver)
    RadioButton radioBtnDriver;
    @BindView(R.id.radioBtnPassenger)
    RadioButton radioBtnPassenger;

    private State state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ParseInstallation.getCurrentInstallation().saveInBackground();
        if (ParseUser.getCurrentUser() != null) {
            transitionToPassengerActivity();
            transitionToDriverRequestListActivity();
        }
        ButterKnife.bind(this);
        state = State.SIGNUP;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logInItem:
                if (state == State.SIGNUP) {
                    state = State.LOGIN;
                    item.setTitle(getResources().getString(R.string.btn_sign_up));
                    btnSignUpOrLogIn.setText(getResources().getString(R.string.btn_log_in));
                } else if (state == State.LOGIN) {
                    state = State.SIGNUP;
                    item.setTitle(getResources().getString(R.string.btn_log_in));
                    btnSignUpOrLogIn.setText(getResources().getString(R.string.btn_sign_up));
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.btnLogInOrSignUp)
    public void onClickBtnLogInOrSignUp() {
        if (state == State.SIGNUP) {
            if (checkIfUserChosenRadioBtn()) {
                signUpNewUser();
            }
        } else if (state == State.LOGIN) {
            logInUser();
        }
    }

    private boolean checkIfUserChosenRadioBtn() {
        if (!radioBtnDriver.isChecked() && !radioBtnPassenger.isChecked()) {
            FancyToast.makeText(MainActivity.this, getResources().getString(R.string.txt_no_registration),
                    Toast.LENGTH_SHORT, FancyToast.CONFUSING, false).show();
            return false;
        } else {
            return true;
        }
    }

    private void signUpNewUser() {
        ParseUser newUser = new ParseUser();
        newUser.setUsername(edtUsername.getText().toString());
        newUser.setPassword(edtPassword.getText().toString());
        newUser.put("as", setRoleForUser());
        newUser.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    FancyToast.makeText(MainActivity.this, "User signed up",
                            Toast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                    transitionToPassengerActivity();
                    transitionToDriverRequestListActivity();
                }
            }
        });
    }

    private String setRoleForUser() {
        if (radioBtnPassenger.isChecked()) {
            return "Passenger";
        } else if (radioBtnDriver.isChecked()) {
            return "Driver";
        } else {
            return null;
        }
    }

    private void logInUser() {
        ParseUser.logInInBackground(edtUsername.getText().toString(),
                edtPassword.getText().toString(), new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException e) {
                        if (user != null && e == null) {
                            FancyToast.makeText(MainActivity.this, "User logged in",
                                    Toast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                            transitionToPassengerActivity();
                            transitionToDriverRequestListActivity();
                        }
                    }
                });
    }

    @OnClick(R.id.btnOneTimeLogin)
    public void onClickBtnOneTimeLogin() {
        if (edtDriverOrPassenger.getText().toString().equals("Driver") ||
                edtDriverOrPassenger.getText().toString().equals("Passenger")) {
            if (ParseUser.getCurrentUser() == null) {
                ParseAnonymousUtils.logIn(new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException e) {
                        if (user != null && e == null) {
                            FancyToast.makeText(MainActivity.this, "Anonymous user",
                                    Toast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                            user.put("as", edtDriverOrPassenger.getText().toString());
                            user.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    transitionToPassengerActivity();
                                    transitionToDriverRequestListActivity();
                                }
                            });
                        }
                    }
                });
            }
        } else {
            FancyToast.makeText(MainActivity.this, getResources().getString(R.string.txt_no_registration),
                    Toast.LENGTH_SHORT, FancyToast.CONFUSING, false).show();
        }
    }

    private void transitionToPassengerActivity() {
        if (ParseUser.getCurrentUser() != null) {
            if (ParseUser.getCurrentUser().get("as").equals("Passenger")) {
                Intent intent = new Intent(MainActivity.this, PassengerActivity.class);
                startActivity(intent);
            }
        }
    }

    private void transitionToDriverRequestListActivity() {
        if (ParseUser.getCurrentUser() != null) {
            if (ParseUser.getCurrentUser().get("as").equals("Driver")) {
                Intent intent = new Intent(MainActivity.this, DriverRequestListActivity.class);
                startActivity(intent);
            }
        }
    }
}
