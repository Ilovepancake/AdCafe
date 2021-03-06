package com.bry.adcafe.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bry.adcafe.Constants;
import com.bry.adcafe.R;
import com.bry.adcafe.Variables;
import com.bry.adcafe.adapters.SubscriptionManagerContainer;
import com.bry.adcafe.services.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mindorks.placeholderview.PlaceHolderView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SubscriptionManager extends AppCompatActivity implements View.OnClickListener{
    public static final String TAG = SelectCategory.class.getSimpleName();
    @Bind(R.id.selectCategoriesLayout) LinearLayout mainView;
    @Bind(R.id.failedLoadLayout) LinearLayout failedToLoadLayout;
    @Bind(R.id.retryLoading) Button retryLoadingButton;
    @Bind(R.id.loadingLayout) LinearLayout loadingLayout;
    @Bind(R.id.categoryPlaceHolderView) PlaceHolderView placeHolderView;
    private Context mContext;
    private ProgressDialog mAuthProgressDialog;

    private boolean isWindowPaused = false;
    private DatabaseReference SKListener;
    private boolean isNeedToLoadLogin = false;

    @Bind(R.id.swipeBackView2)View swipeBackView2;
    private boolean isSwipingForBack2 = false;
    private GestureDetector mSwipeBackDetector2;
    private int maxSideSwipeLength2 = 200;
    private int x_delta2;
    private List<Integer> SideSwipeRawList2 = new ArrayList<>();
    private boolean isSideScrolling2 = false;
    private int maxSideSwipeLength = 200;

    @Bind(R.id.title) TextView title;
    @Bind(R.id.explanation) TextView explanation;
    private int mAnimationTime = 300;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_manager);

        mContext = this.getApplicationContext();
        ButterKnife.bind(this);
        createProgressDialog();
        registerAllReceivers();
        retryLoadingButton.setOnClickListener(this);
        if (isOnline(mContext)) loadCategoriesFromFirebase();
        else{
            mainView.setVisibility(View.GONE);
            failedToLoadLayout.setVisibility(View.VISIBLE);
        }

        addTouchListenerForSwipeBack2();
    }

    @Override
    protected void onDestroy(){
        unregisterAllReceivers();
        super.onDestroy();
    }

    @Override
    protected void onResume(){
        isWindowPaused = false;
        super.onResume();
        if(isNeedToLoadLogin){
            Intent intent = new Intent(SubscriptionManager.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }else addListenerForChangeInSessionKey();

    }

    @Override
    protected void onPause(){
        removeListenerForChangeInSessionKey();
        isWindowPaused = true;
        super.onPause();
    }

    private void unregisterAllReceivers(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverForFinishedUnSubscribing);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverForFinishedSubscribing);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverForConfirmSubscribing);
        Intent intent = new Intent("UNREGISTER_ALL");
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private void registerAllReceivers(){
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiverForFinishedUnSubscribing,
                new IntentFilter(Constants.FINISHED_UNSUBSCRIBING)); //this receives message for finished un-subscribing user.

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiverForFinishedSubscribing,
                new IntentFilter(Constants.SET_UP_USERS_SUBSCRIPTION_LIST)); //this receives message for finished subscribing user.

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiverForConfirmSubscribing,
                new IntentFilter(Constants.CONFIRM_START)); //this receives message to prompt user if they are sure to continue action.
    }

    private void loadCategoriesFromFirebase() {
        failedToLoadLayout.setVisibility(View.GONE);
        mainView.setVisibility(View.GONE);
        loadingLayout.setVisibility(View.VISIBLE);

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(Constants.CATEGORY_LIST);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snap:dataSnapshot.getChildren()){
                    String category = snap.getKey();
                    List<String> subcategories = new ArrayList<>();
                    for(DataSnapshot subSnap: snap.getChildren()){
                        String cat = subSnap.getValue(String.class);
                        if(doesCategoryImageExist(cat)) subcategories.add(cat);
                    }
                    if(!subcategories.isEmpty()){
                        if(!category.equals(Constants.CATEGORY_EVERYONE_CONTAINER)) {
                            placeHolderView.addView(new SubscriptionManagerContainer(mContext, placeHolderView, category, subcategories));
                        }
                    }
                }
                loadingLayout.setVisibility(View.GONE);
                mainView.setVisibility(View.VISIBLE);

                title.animate().translationX(0).setDuration(mAnimationTime).setInterpolator(new LinearOutSlowInInterpolator())
                        .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                title.setTranslationX(0);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        }).start();

                explanation.animate().translationX(0).setDuration(mAnimationTime).setInterpolator(new LinearOutSlowInInterpolator())
                        .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                explanation.setTranslationX(0);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        }).start();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                failedToLoadLayout.setVisibility(View.VISIBLE);
                loadingLayout.setVisibility(View.GONE);
            }
        });
    }

    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in airplane mode it will be null
        return (netInfo != null && netInfo.isConnected());
    }

    @Override
    public void onClick(View v) {
        if(v==retryLoadingButton){
            if(isOnline(mContext)) {
                loadCategoriesFromFirebase();
            }else{
                Toast.makeText(mContext,"To continue,you need an internet connection",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private BroadcastReceiver mMessageReceiverForFinishedUnSubscribing = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Finished updating data.");
            dismissProgressDialog();

        }
    };

    private BroadcastReceiver mMessageReceiverForFinishedSubscribing = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Finished updating data.");
            dismissProgressDialog();
        }
    };


    private BroadcastReceiver mMessageReceiverForConfirmSubscribing = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Message received for confirm subscribing or unsubscribing.");
            showConfirmSubscribeMessage();
        }
    };

    private void showConfirmSubscribeMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("AdCafe.");
        builder.setMessage(Variables.areYouSureText)
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Intent intent = new Intent(Constants.CANCELLED);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                    }
                })
                .setPositiveButton("Yeah!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Constants.ALL_CLEAR);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                        showProgressDialog();
                    }
                })
                .setNegativeButton("No.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Constants.CANCELLED);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                        dialog.cancel();
                    }
                }).show();
    }

    private void showProgressDialog() {
        mAuthProgressDialog.show();
    }

    private void dismissProgressDialog() {
        mAuthProgressDialog.dismiss();
    }

    private void createProgressDialog(){
        mAuthProgressDialog = new ProgressDialog(this,R.style.AppCompatAlertDialogStyle);
        mAuthProgressDialog.setTitle(R.string.app_name);
        mAuthProgressDialog.setMessage("Updating your preferences...");
        mAuthProgressDialog.setCancelable(false);
    }

    private String getSubscriptionValue(int index) {
        LinkedHashMap map = Variables.Subscriptions;
        String Sub = (new ArrayList<String>(map.keySet())).get(index);
        Log.d("SubscriptionManagerItem", "Subscription gotten from getCurrent Subscription method is :" + Sub);
        return Sub;
    }

    private int getPositionOf(String subscription) {
        LinkedHashMap map = Variables.Subscriptions;
        List<String> indexes = new ArrayList<String>(map.keySet());
        return indexes.indexOf(subscription);
    }

    private void updateCurrentSubIndex(){
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference adRef3 = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_CHILD_USERS)
                .child(uid).child(Constants.CURRENT_SUBSCRIPTION_INDEX);
        Log.d(TAG,"Setting current subscription index in firebase to :"+Variables.getCurrentSubscriptionIndex());
        adRef3.setValue(Variables.getCurrentSubscriptionIndex());
    }



    ChildEventListener chil = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            if(dataSnapshot.getKey().equals(Constants.BOI_IS_DA_KEY)){
                String firebasekey = dataSnapshot.getValue(String.class);
                if(!firebasekey.equals(getSessionKey())){
                    PerformShutdown();
                }
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    public void addListenerForChangeInSessionKey(){
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference FirstCheckref = FirebaseDatabase.getInstance().getReference().child(Constants.FIREBASE_CHILD_USERS)
                .child(uid).child(Constants.BOI_IS_DA_KEY);
        FirstCheckref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    String firebasekey = dataSnapshot.getValue(String.class);
                    if (!firebasekey.equals(getSessionKey())) {
                        PerformShutdown();
                    }else{
                        nowReallyAddLisenerForChangeInSessionKey();
                    }
                }else{
                    PerformShutdown();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void nowReallyAddLisenerForChangeInSessionKey(){
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SKListener = FirebaseDatabase.getInstance().getReference().child(Constants.FIREBASE_CHILD_USERS)
                .child(uid);
        SKListener.addChildEventListener(chil);
    }

    public void removeListenerForChangeInSessionKey(){
        if(SKListener!=null){
            SKListener.removeEventListener(chil);
        }
    }

    public String getSessionKey(){
        SharedPreferences prefs2 = getSharedPreferences(Constants.BOI_IS_DA_KEY, MODE_PRIVATE);
        String sk = prefs2.getString(Constants.BOI_IS_DA_KEY, "NULL");
        Log.d(TAG, "Loading session key from shared prefs - " + sk);
        return sk;
    }



    public void PerformShutdown(){
        if (FirebaseAuth.getInstance() != null) {
            FirebaseAuth.getInstance().signOut();
        }
        Variables.resetAllValues();
        if(!isWindowPaused){
            Intent intent = new Intent(SubscriptionManager.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }else{
            isNeedToLoadLogin = true;
        }

    }

    private boolean doesCategoryImageExist(String category){
        String filename;
        filename = category.replaceAll(" ","_");
        int res = mContext.getResources().getIdentifier(filename, "drawable", mContext.getPackageName());
        if(res==0)Log.d(TAG,"Category image for "+category+" does not exist");
        return res != 0;
    }



    private void addTouchListenerForSwipeBack2() {
        mSwipeBackDetector2 = new GestureDetector(this, new MySwipebackGestureListener2());
        swipeBackView2.setOnTouchListener(touchListener2);
    }

    View.OnTouchListener touchListener2 = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (mSwipeBackDetector2.onTouchEvent(motionEvent)) {
                return true;
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                if (isSideScrolling2) {
                    Log.d("touchListener", " onTouch ACTION_UP");
                    isSideScrolling2 = false;

                    int RightScore = 0;
                    int LeftScore = 0;
                    if (SideSwipeRawList2.size() > 15) {
                        for (int i = SideSwipeRawList2.size() - 1; i > SideSwipeRawList2.size() - 15; i--) {
                            int num1 = SideSwipeRawList2.get(i);
                            int numBefore1 = SideSwipeRawList2.get(i - 1);

                            if (numBefore1 > num1) LeftScore++;
                            else RightScore++;
                        }
                    } else {
                        for (int i = SideSwipeRawList2.size() - 1; i > 0; i--) {
                            int num1 = SideSwipeRawList2.get(i);
                            int numBefore1 = SideSwipeRawList2.get(i - 1);

                            if (numBefore1 > num1) LeftScore++;
                            else RightScore++;
                        }
                    }
                    if (RightScore > LeftScore) {
//                        onBackPressed();
                    }
                    hideNupdateSideSwipeThing2();
                    SideSwipeRawList2.clear();
                }
                ;
            }

            return false;
        }
    };

    class MySwipebackGestureListener2 extends GestureDetector.SimpleOnGestureListener {
        int origX = 0;
        int origY = 0;


        @Override
        public boolean onDown(MotionEvent event) {
            Log.d("TAG", "onDown: ");
            final int X = (int) event.getRawX();
            final int Y = (int) event.getRawY();
            Log.d("TAG", "onDown: event.getRawX(): " + event.getRawX() + " event.getRawY()" + event.getRawY());
            CoordinatorLayout.LayoutParams lParams = (CoordinatorLayout.LayoutParams) swipeBackView2.getLayoutParams();
            x_delta2 = X - lParams.leftMargin;

            origX = lParams.leftMargin;
            origY = lParams.topMargin;


            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.i("TAG", "onSingleTapConfirmed: ");
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.i("TAG", "onLongPress: ");
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.i("TAG", "onDoubleTap: ");
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            final int Y = (int) e2.getRawY();
            final int X = (int) e2.getRawX();
            if ((X - x_delta2) < maxSideSwipeLength) {

            } else if ((X - x_delta2) > maxSideSwipeLength) {

            } else {
            }
            showNupdateSideSwipeThing2(X - x_delta2);

            Log.d("TAG", "the e2.getAction()= " + e2.getAction() + " and the MotionEvent.ACTION_CANCEL= " + MotionEvent.ACTION_CANCEL);
            SideSwipeRawList2.add(X);

            isSideScrolling2 = true;
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            int RightScore = 0;
            int LeftScore = 0;
            if (SideSwipeRawList2.size() > 15) {
                for (int i = SideSwipeRawList2.size() - 1; i > SideSwipeRawList2.size() - 15; i--) {
                    int num1 = SideSwipeRawList2.get(i);
                    int numBefore1 = SideSwipeRawList2.get(i - 1);

                    if (numBefore1 > num1) LeftScore++;
                    else RightScore++;
                }
            } else {
                for (int i = SideSwipeRawList2.size() - 1; i > 0; i--) {
                    int num1 = SideSwipeRawList2.get(i);
                    int numBefore1 = SideSwipeRawList2.get(i - 1);

                    if (numBefore1 > num1) LeftScore++;
                    else RightScore++;
                }
            }
            if (RightScore > LeftScore) {
                onBackPressed();
            }
            SideSwipeRawList2.clear();
            return false;

        }
    }

    private void showNupdateSideSwipeThing2(int pos) {
        int trans = (int) ((pos - Utils.dpToPx(10)) * 0.9);
//        RelativeLayout isGoingBackIndicator = findViewById(R.id.isGoingBackIndicator);
//        isGoingBackIndicator.setTranslationX(trans);

        View v = findViewById(R.id.swipeBackViewIndicator2);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) v.getLayoutParams();
        params.height = trans;
        v.setLayoutParams(params);

        mainView.setTranslationX((int)(trans*0.05));

    }

    private void hideNupdateSideSwipeThing2() {
        int myDurat = 200;
//        RelativeLayout isGoingBackIndicator = findViewById(R.id.isGoingBackIndicator);
//        isGoingBackIndicator.animate().setDuration(mAnimationTime).translationX(Utils.dpToPx(-40)).start();

        final View v = findViewById(R.id.swipeBackViewIndicator2);
        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) v.getLayoutParams();

        ValueAnimator animatorTop;
        animatorTop = ValueAnimator.ofInt(params.height, 0);
        animatorTop.setInterpolator(new LinearOutSlowInInterpolator());
        animatorTop.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                params.height = (Integer) valueAnimator.getAnimatedValue();
                v.requestLayout();
            }
        });
        animatorTop.setDuration(myDurat).start();


        mainView.animate().setDuration(myDurat).setInterpolator(new LinearOutSlowInInterpolator()).translationX(0)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mainView.setTranslationX(0);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).start();
    }

}
