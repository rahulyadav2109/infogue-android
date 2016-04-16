package com.sketchproject.infogue.fragments;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.sketchproject.infogue.R;
import com.sketchproject.infogue.activities.AuthenticationActivity;
import com.sketchproject.infogue.activities.ProfileActivity;
import com.sketchproject.infogue.modules.SessionManager;
import com.sketchproject.infogue.modules.Validator;
import com.sketchproject.infogue.utils.Constant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends Fragment implements Validator.ViewValidation {

    private UserLoginTask mAuthTask = null;
    private Validator validator;
    private AlertFragment alert;

    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private String username;
    private String password;
    private List<String> validationMessage;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        validator = new Validator();
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mUsernameView = (EditText) getActivity().findViewById(R.id.input_username);
        mUsernameView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mPasswordView = (EditText) getActivity().findViewById(R.id.input_password);

        Button mEmailSignInButton = (Button) getActivity().findViewById(R.id.btn_sign_in);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        TextView mCreateAccountButton = (TextView) getActivity().findViewById(R.id.btn_create_account);
        mCreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity mActivity = getActivity();
                if (mActivity instanceof AuthenticationActivity) {
                    ((AuthenticationActivity) getActivity()).setTabRegisterActive();
                }
            }
        });

        ImageButton mFacebookButton = (ImageButton) getActivity().findViewById(R.id.btn_facebook);
        mFacebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        ImageButton mTwitterButton = (ImageButton) getActivity().findViewById(R.id.btn_twitter);
        mTwitterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        TextView mForgotPassword = (TextView) getActivity().findViewById(R.id.btn_forgot);
        mForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constant.URL_FORGOT));
                startActivity(browserIntent);
            }
        });

        mLoginFormView = getActivity().findViewById(R.id.login_form);
        mProgressView = getActivity().findViewById(R.id.login_progress);

        alert = (AlertFragment) getChildFragmentManager().findFragmentById(R.id.alert_fragment);
    }

    private void attemptLogin() {
        // Escape if mAuthTask is defined it means login still process.
        if (mAuthTask != null) {
            return;
        }

        preValidation();
        postValidation(onValidateView());
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int mediumAnimTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView
                .animate()
                .setDuration(mediumAnimTime)
                .alpha(show ? 0 : 1)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                    }
                });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate()
                .setDuration(mediumAnimTime)
                .alpha(show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private boolean demoOnly() {
        SessionManager sessionManager = new SessionManager(getActivity().getBaseContext());
        HashMap<String, Object> user = new HashMap<>();
        user.put(SessionManager.KEY_ID, 1);
        user.put(SessionManager.KEY_TOKEN, "6224573472634636534");
        user.put(SessionManager.KEY_USERNAME, "anggadarkprince");
        user.put(SessionManager.KEY_NAME, "Angga Ari Wijaya");
        user.put(SessionManager.KEY_LOCATION, "Gresik, Indonesia");
        user.put(SessionManager.KEY_ABOUT, "Freelancer UI/UX Designer, The wind at my back so it's time to fly. angga-ari.com");
        user.put(SessionManager.KEY_AVATAR, "http://infogue.id/images/contributors/avatar_1.jpg");
        user.put(SessionManager.KEY_COVER, "http://infogue.id/images/covers/cover_5.jpg");
        user.put(SessionManager.KEY_ARTICLE, 234);
        user.put(SessionManager.KEY_FOLLOWER, 362);
        user.put(SessionManager.KEY_FOLLOWING, 345);
        return sessionManager.createLoginSession(user);
    }

    @Override
    public void preValidation() {
        // Store values at the time of the login attempt.
        username = mUsernameView.getText().toString();
        password = mPasswordView.getText().toString();
        validationMessage = new ArrayList<>();
    }

    @Override
    public boolean onValidateView() {
        boolean isInvalid = false;
        View focusView = null;

        if (validator.isEmpty(password, true)) {
            validationMessage.add(getString(R.string.error_password_required));
            focusView = mPasswordView;
            isInvalid = true;
        }

        if (validator.isEmpty(username, true)) {
            validationMessage.add(getString(R.string.error_username_required));
            focusView = mUsernameView;
            isInvalid = true;
        }

        if (isInvalid) {
            focusView.requestFocus();
        }

        return !isInvalid;
    }

    @Override
    public void postValidation(boolean isValid) {
        if (isValid) {
            alert.dismiss();
            showProgress(true);
            mAuthTask = new UserLoginTask(username, password);
            mAuthTask.execute((Void) null);
        } else {
            alert.setAlertType(AlertFragment.ALERT_WARNING);
            alert.setAlertMessage(validationMessage);
            alert.show();
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mPassword;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Simulate network access.
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return false;
            }

            return mUsername.equals("angga") && mPassword.equals("angga1234");
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
                if (demoOnly()) {
                    getActivity().finish();
                    Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                    SessionManager session = new SessionManager(getContext());
                    profileIntent.putExtra(SessionManager.KEY_ID, session.getSessionData(SessionManager.KEY_ID, 0));
                    profileIntent.putExtra(SessionManager.KEY_USERNAME, session.getSessionData(SessionManager.KEY_USERNAME, null));
                    profileIntent.putExtra(SessionManager.KEY_NAME, session.getSessionData(SessionManager.KEY_NAME, null));
                    profileIntent.putExtra(SessionManager.KEY_LOCATION, session.getSessionData(SessionManager.KEY_LOCATION, null));
                    profileIntent.putExtra(SessionManager.KEY_ABOUT, session.getSessionData(SessionManager.KEY_ABOUT, null));
                    profileIntent.putExtra(SessionManager.KEY_AVATAR, session.getSessionData(SessionManager.KEY_AVATAR, null));
                    profileIntent.putExtra(SessionManager.KEY_COVER, session.getSessionData(SessionManager.KEY_COVER, null));
                    profileIntent.putExtra(SessionManager.KEY_ARTICLE, session.getSessionData(SessionManager.KEY_ARTICLE, 0));
                    profileIntent.putExtra(SessionManager.KEY_FOLLOWER, session.getSessionData(SessionManager.KEY_FOLLOWER, 0));
                    profileIntent.putExtra(SessionManager.KEY_FOLLOWING, session.getSessionData(SessionManager.KEY_FOLLOWING, 0));
                    profileIntent.putExtra(SessionManager.KEY_IS_FOLLOWING, false);
                    profileIntent.putExtra(AuthenticationActivity.AFTER_LOGIN, true);
                    profileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    profileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(profileIntent);
                } else {
                    Toast.makeText(getContext(), "Something is getting wrong", Toast.LENGTH_LONG).show();
                }

            } else {
                showProgress(false);
                mPasswordView.requestFocus();
                AlertFragment fragment = (AlertFragment) getChildFragmentManager().findFragmentById(R.id.alert_fragment);
                fragment.setAlertType(AlertFragment.ALERT_DANGER);
                fragment.setAlertMessage(getString(R.string.error_auth_credential));
                fragment.show();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
