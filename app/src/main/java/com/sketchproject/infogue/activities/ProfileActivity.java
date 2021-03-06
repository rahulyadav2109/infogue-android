package com.sketchproject.infogue.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.sketchproject.infogue.R;
import com.sketchproject.infogue.events.ArticleContextBuilder;
import com.sketchproject.infogue.events.ArticleListEvent;
import com.sketchproject.infogue.events.ArticlePopupBuilder;
import com.sketchproject.infogue.events.FollowerListEvent;
import com.sketchproject.infogue.fragments.ArticleFragment;
import com.sketchproject.infogue.models.Article;
import com.sketchproject.infogue.models.Contributor;
import com.sketchproject.infogue.models.Message;
import com.sketchproject.infogue.modules.ConnectionDetector;
import com.sketchproject.infogue.modules.SessionManager;
import com.sketchproject.infogue.modules.VolleySingleton;
import com.sketchproject.infogue.utils.APIBuilder;
import com.sketchproject.infogue.utils.Helper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A {@link AppCompatActivity} subclass, show contributor profile.
 * <p/>
 * Sketch Project Studio
 * Created by Angga on 15/012/2016 10.37
 */
public class ProfileActivity extends AppCompatActivity implements ArticleFragment.OnArticleInteractionListener {

    public static final int PROFILE_RESULT_CODE = 200;

    private SessionManager session;
    private ConnectionDetector connectionDetector;

    private TextView mArticleView;
    private TextView mFollowerView;
    private TextView mFollowingView;

    private Contributor contributor;
    private String username;
    private boolean isFollowing;
    private boolean isAfterLogin;

    private Contributor tempContributor;
    private View tempButtonFollow;

    /**
     * Perform initialization of ProfileActivity.
     *
     * @param savedInstanceState saved last state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(getBaseContext());
        connectionDetector = new ConnectionDetector(getBaseContext());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView mNameView = (TextView) findViewById(R.id.name);
        TextView mLocationView = (TextView) findViewById(R.id.location);
        TextView mAboutView = (TextView) findViewById(R.id.about);
        ImageView mAvatarImage = (ImageView) findViewById(R.id.avatar);
        ImageView mCoverImage = (ImageView) findViewById(R.id.cover);

        mArticleView = (TextView) findViewById(R.id.valueArticle);
        mFollowerView = (TextView) findViewById(R.id.valueFollower);
        mFollowingView = (TextView) findViewById(R.id.valueFollowing);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int contributorId = extras.getInt(SessionManager.KEY_ID);
            username = extras.getString(SessionManager.KEY_USERNAME);
            isFollowing = extras.getBoolean(SessionManager.KEY_IS_FOLLOWING);

            contributor = new Contributor();
            contributor.setId(contributorId);
            contributor.setUsername(username);
            contributor.setIsFollowing(isFollowing);
            contributor.setName(extras.getString(SessionManager.KEY_NAME));
            contributor.setAvatar(extras.getString(SessionManager.KEY_AVATAR));

            String status = extras.getString(SessionManager.KEY_STATUS, "invalid");
            if (!status.equals(Contributor.STATUS_ACTIVATED)) {
                int color = R.color.color_danger_transparent;
                if (status.equals(Contributor.STATUS_PENDING)) {
                    color = R.color.color_warning_transparent;
                }
                Helper.toastColor(getBaseContext(), "Contributor is " + status, color);
                Intent returnIntent = new Intent();
                setResult(AppCompatActivity.RESULT_CANCELED, returnIntent);
                finish();
            }

            if (mNameView != null) {
                mNameView.setText(extras.getString(SessionManager.KEY_NAME));
            }
            if (mLocationView != null) {
                mLocationView.setText(extras.getString(SessionManager.KEY_LOCATION));
            }
            if (mAboutView != null) {
                mAboutView.setText(extras.getString(SessionManager.KEY_ABOUT));
            }
            if (mArticleView != null) {
                mArticleView.setText(String.valueOf(extras.getInt(SessionManager.KEY_ARTICLE)));
            }
            if (mFollowerView != null) {
                mFollowerView.setText(String.valueOf(extras.getInt(SessionManager.KEY_FOLLOWER)));
            }
            if (mFollowingView != null) {
                mFollowingView.setText(String.valueOf(extras.getInt(SessionManager.KEY_FOLLOWING)));
            }

            if (mAvatarImage != null) {
                Glide.clear(mAvatarImage);
                Glide.with(this)
                        .load(extras.getString(SessionManager.KEY_AVATAR))
                        .placeholder(R.drawable.placeholder_square)
                        .dontAnimate()
                        .into(mAvatarImage);
            }

            if (mCoverImage != null) {
                Glide.clear(mCoverImage);
                Glide.with(this)
                        .load(extras.getString(SessionManager.KEY_COVER))
                        .placeholder(R.drawable.placeholder_rectangle)
                        .centerCrop()
                        .crossFade()
                        .into(mCoverImage);
            }

            isAfterLogin = extras.getBoolean(AuthenticationActivity.AFTER_LOGIN);

            buildProfileEventHandler(contributorId, username);
        } else {
            Helper.toastColor(getBaseContext(), "Invalid user profile", R.color.color_danger_transparent);
            Intent returnIntent = new Intent();
            setResult(AppCompatActivity.RESULT_CANCELED, returnIntent);
            finish();
        }
    }

    /**
     * Build event and display buttons depend on profile info.
     *
     * @param idContributor       profile id
     * @param usernameContributor profile username
     */
    private void buildProfileEventHandler(final int idContributor, final String usernameContributor) {
        // set action bar title with opened username
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(usernameContributor);
        }

        // set listener for article stat to open detail of article of this user
        View mArticleButton = findViewById(R.id.btn_article);
        if (mArticleButton != null) {
            mArticleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (connectionDetector.isNetworkAvailable()) {
                        Intent articleIntent = new Intent(getBaseContext(), ArticleActivity.class);
                        articleIntent.putExtra(SessionManager.KEY_ID, idContributor);
                        articleIntent.putExtra(SessionManager.KEY_USERNAME, usernameContributor);
                        startActivity(articleIntent);
                    } else {
                        connectionDetector.snackbarDisconnectNotification(findViewById(R.id.scroll_container), null);
                    }
                }
            });
        }

        // set listener for follower stat to open detail of followers of this user
        View mFollowerButton = findViewById(R.id.btn_followers);
        if (mFollowerButton != null) {
            mFollowerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (connectionDetector.isNetworkAvailable()) {
                        Intent followerIntent = new Intent(getBaseContext(), FollowerActivity.class);
                        followerIntent.putExtra(FollowerActivity.SCREEN_REQUEST, FollowerActivity.FOLLOWER_SCREEN);
                        followerIntent.putExtra(SessionManager.KEY_ID, idContributor);
                        followerIntent.putExtra(SessionManager.KEY_USERNAME, usernameContributor);
                        startActivity(followerIntent);
                    } else {
                        connectionDetector.snackbarDisconnectNotification(findViewById(R.id.scroll_container), null);
                    }
                }
            });
        }

        // set listener for following stats to open detail of following of this user
        View mFollowingButton = findViewById(R.id.btn_following);
        if (mFollowingButton != null) {
            mFollowingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (connectionDetector.isNetworkAvailable()) {
                        Intent followingIntent = new Intent(getBaseContext(), FollowerActivity.class);
                        followingIntent.putExtra(FollowerActivity.SCREEN_REQUEST, FollowerActivity.FOLLOWING_SCREEN);
                        followingIntent.putExtra(SessionManager.KEY_ID, idContributor);
                        followingIntent.putExtra(SessionManager.KEY_USERNAME, usernameContributor);
                        startActivity(followingIntent);
                    } else {
                        connectionDetector.snackbarDisconnectNotification(findViewById(R.id.scroll_container), null);
                    }
                }
            });
        }

        // initialize control button such as message, detail and info (open web)
        final Button mDetailButton = (Button) findViewById(R.id.btn_detail);
        final ImageButton mMessageButton = (ImageButton) findViewById(R.id.btn_message);
        final ImageButton mInfoButton = (ImageButton) findViewById(R.id.btn_info);
        final Button mFollowButton = (Button) findViewById(R.id.btn_follow_control);

        // Open my profile if the user id was passed from another activity is equal with current session
        // try to update profile, change info button with detail button (larger) info and hide
        // follow control and message button
        int loggedUserId = session.getSessionData(SessionManager.KEY_ID, 0);
        if (session.isLoggedIn() && loggedUserId == idContributor) {
            updateProfileInBackground();

            if (mDetailButton != null) {
                mDetailButton.setVisibility(View.VISIBLE);
                mDetailButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String url = APIBuilder.getContributorDetailUrl(usernameContributor);
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(browserIntent);
                    }
                });
            }
            if (mFollowButton != null) {
                mFollowButton.setVisibility(View.GONE);
            }
            if (mMessageButton != null) {
                mMessageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent messageIntent = new Intent(getBaseContext(), MessageActivity.class);
                        startActivity(messageIntent);
                    }
                });
            }
            if (mInfoButton != null) {
                mInfoButton.setVisibility(View.GONE);
            }
        } else {
            // Open another contributor, prefer small button info instead detail button
            // show follow control and message button as well
            if (mDetailButton != null) {
                mDetailButton.setVisibility(View.GONE);
            }
            if (mFollowButton != null) {
                mFollowButton.setVisibility(View.VISIBLE);
                mFollowButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // able to follow people if they logged in and make sure network is available
                        if (session.isLoggedIn()) {
                            if (connectionDetector.isNetworkAvailable()) {
                                toggleFollowHandler(mFollowButton);
                            } else {
                                connectionDetector.snackbarDisconnectNotification(findViewById(R.id.scroll_container), null);
                            }
                        } else {
                            // instead bring me a login screen
                            Intent authIntent = new Intent(getBaseContext(), AuthenticationActivity.class);
                            startActivity(authIntent);
                        }
                    }
                });
            }
            if (mMessageButton != null) {
                mMessageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent conversationIntent = new Intent(ProfileActivity.this, ConversationActivity.class);
                        conversationIntent.putExtra(Message.USERNAME, contributor.getUsername());
                        conversationIntent.putExtra(Message.CONTRIBUTOR_ID, contributor.getId());
                        conversationIntent.putExtra(Message.NAME, contributor.getName());
                        conversationIntent.putExtra(Message.AVATAR, contributor.getAvatar());
                        conversationIntent.putExtra(ConversationActivity.NEW_CONVERSATION, false);
                        startActivity(conversationIntent);
                    }
                });
            }
            if (mInfoButton != null) {
                mInfoButton.setVisibility(View.VISIBLE);
                mInfoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String url = APIBuilder.getContributorDetailUrl(usernameContributor);
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(browserIntent);
                    }
                });
            }

            // set current following status by check idFollowing variable that passed from activity
            if (isFollowing) {
                stateFollow(mFollowButton);
            } else {
                stateUnfollow(mFollowButton);
            }
        }

        // replacing stream fragment
        Fragment fragment = ArticleFragment.newInstanceStream(1, loggedUserId, username);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment, fragment);
        fragmentTransaction.commit();
    }

    /**
     * Toggle follow control. User make changing of follow state with someone,
     * if they followed before then now they are not following anymore, vice versa.
     */
    private void toggleFollowHandler(View followButton) {
        tempContributor = FollowerListEvent.contributor;
        tempButtonFollow = FollowerListEvent.followButton;
        new FollowerListEvent(this, contributor, followButton).followContributor();
        isFollowing = !isFollowing;
    }

    /**
     * Update session or profile info.
     * While user create new article, follow people then need to update session info,
     * such as stats but silently and do not return any error messages.
     */
    private void updateProfileInBackground() {
        JsonObjectRequest contributorRequest = new JsonObjectRequest(Request.Method.GET, APIBuilder.getApiContributorUrl(username), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String status = response.getString(APIBuilder.RESPONSE_STATUS);
                            JSONObject contributor = response.getJSONObject(Contributor.DATA);

                            if (status.equals(APIBuilder.REQUEST_SUCCESS)) {
                                session.setSessionData(SessionManager.KEY_ARTICLE, contributor.getInt(Contributor.ARTICLE));
                                session.setSessionData(SessionManager.KEY_FOLLOWER, contributor.getInt(Contributor.FOLLOWERS));
                                session.setSessionData(SessionManager.KEY_FOLLOWING, contributor.getInt(Contributor.FOLLOWING));

                                mArticleView.setText(String.valueOf(session.getSessionData(SessionManager.KEY_ARTICLE, 0)));
                                mFollowerView.setText(String.valueOf(session.getSessionData(SessionManager.KEY_FOLLOWER, 0)));
                                mFollowingView.setText(String.valueOf(session.getSessionData(SessionManager.KEY_FOLLOWING, 0)));
                            } else {
                                Log.w("Infogue/Profile", getString(R.string.error_unknown));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();

                        String errorMessage = getString(R.string.error_unknown);
                        if (error.networkResponse == null) {
                            if (error.getClass().equals(TimeoutError.class)) {
                                errorMessage = getString(R.string.error_timeout);
                            } else if (error.getClass().equals(NoConnectionError.class)) {
                                errorMessage = getString(R.string.error_no_connection);
                            }
                        } else {
                            errorMessage = getString(R.string.error_server);
                        }
                        Log.e("Infogue/Profile", errorMessage);
                    }
                }
        );
        contributorRequest.setRetryPolicy(new DefaultRetryPolicy(
                APIBuilder.TIMEOUT_SHORT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleySingleton.getInstance(getBaseContext()).addToRequestQueue(contributorRequest);
    }

    /**
     * Change button follow control into follow state. after making request to following someone.
     *
     * @param mFollowButton follow control button
     */
    private void stateFollow(Button mFollowButton) {
        mFollowButton.setBackgroundResource(R.drawable.btn_primary);
        mFollowButton.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.light));
        mFollowButton.setText(getString(R.string.action_unfollow));
    }

    /**
     * Current state is following and turns into unfollow, when user tap on this state will change
     * into filled button and now they are following someone.
     *
     * @param mFollowButton follow control button
     */
    private void stateUnfollow(Button mFollowButton) {
        mFollowButton.setBackgroundResource(R.drawable.btn_toggle);
        mFollowButton.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.primary));
        mFollowButton.setText(getString(R.string.action_follow));
    }

    /**
     * Create option menu.
     *
     * @param menu content of option menu
     * @return boolean
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.info, menu);
        return true;
    }

    /**
     * Select action for option menu.
     *
     * @param item of selected option menu
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            if (isAfterLogin) {
                launchMainActivity();
            } else {
                if (tempContributor != null && tempButtonFollow != null) {
                    FollowerListEvent.contributor = tempContributor;
                    FollowerListEvent.followButton = tempButtonFollow;
                }

                Intent returnIntent = new Intent();
                returnIntent.putExtra(SessionManager.KEY_IS_FOLLOWING, isFollowing);
                setResult(AppCompatActivity.RESULT_OK, returnIntent);
                finish();
            }
        } else if (id == R.id.action_feedback) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(APIBuilder.URL_FEEDBACK));
            startActivity(browserIntent);
        } else if (id == R.id.action_help) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(APIBuilder.URL_HELP));
            startActivity(browserIntent);
        } else if (id == R.id.action_rating) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(APIBuilder.URL_APP));
            startActivity(browserIntent);
        } else if (id == R.id.action_about) {
            Intent aboutActivity = new Intent(getBaseContext(), AboutActivity.class);
            startActivity(aboutActivity);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * When user press back destroy and return the status or re launch ApplicationActivity
     */
    @Override
    public void onBackPressed() {
        if (isAfterLogin) {
            launchMainActivity();
        } else {
            if (tempContributor != null && tempButtonFollow != null) {
                FollowerListEvent.contributor = tempContributor;
                FollowerListEvent.followButton = tempButtonFollow;
            }

            Intent returnIntent = new Intent();
            returnIntent.putExtra(SessionManager.KEY_IS_FOLLOWING, isFollowing);
            setResult(AppCompatActivity.RESULT_OK, returnIntent);
        }

        super.onBackPressed();
    }

    /**
     * After login we need to launch new ApplicationActivity
     * because it never exist or has been destroyed.
     */
    private void launchMainActivity() {
        Intent applicationIntent = new Intent(getBaseContext(), ApplicationActivity.class);
        applicationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        applicationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(applicationIntent);
        finish();
    }

    @Override
    public void onArticleInteraction(View view, Article article) {
        new ArticleListEvent(this, article)
                .viewArticle();
    }

    @Override
    public void onArticlePopupInteraction(View view, Article article) {
        new ArticlePopupBuilder(this, view, article)
                .buildPopup()
                .show();
    }

    @Override
    public void onArticleLongClickInteraction(View view, Article article) {
        new ArticleContextBuilder(this, article)
                .buildContext()
                .show();
    }
}
