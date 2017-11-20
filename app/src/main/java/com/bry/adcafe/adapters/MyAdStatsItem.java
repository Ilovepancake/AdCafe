package com.bry.adcafe.adapters;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.bry.adcafe.R;
import com.bry.adcafe.models.Advert;
import com.mindorks.placeholderview.PlaceHolderView;
import com.mindorks.placeholderview.annotations.Layout;
import com.mindorks.placeholderview.annotations.NonReusable;
import com.mindorks.placeholderview.annotations.Resolve;
import com.mindorks.placeholderview.annotations.View;

/**
 * Created by bryon on 19/11/2017.
 */


@NonReusable
@Layout(R.layout.my_ad_stat_item)
public class MyAdStatsItem {
    @View(R.id.adImage) private ImageView mAdImage;
    @View(R.id.EmailText) private TextView mEmail;
    @View(R.id.TargetedNumber) private TextView mTargetedNumber;
    @View(R.id.usersReachedSoFar) private TextView mUsersReachedSoFar;
    @View(R.id.AmountToReimburse) private TextView mAmountToReimburse;

    private Context mContext;
    private PlaceHolderView mPlaceHolderView;
    private Advert mAdvert;

    public MyAdStatsItem(Context Context, PlaceHolderView PlaceHolderView, Advert Advert){
        this.mContext = Context;
        this.mPlaceHolderView = PlaceHolderView;
        this.mAdvert = Advert;
    }

    @Resolve
    public void onResolved(){
        mEmail.setText("Uploaded by : "+mAdvert.getUserEmail());
        mTargetedNumber.setText("No. of users targeted : "+mAdvert.getNumberOfUsersToReach());
        mUsersReachedSoFar.setText("Users reached so far : "+mAdvert.getNumberOfTimesSeen());

        int numberOfUsersWhoDidntSeeAd = mAdvert.getNumberOfUsersToReach()- mAdvert.getNumberOfTimesSeen();
        String number = Integer.toString(numberOfUsersWhoDidntSeeAd);
        mAmountToReimburse.setText("Amount to be reimbursed : "+number+" Ksh");
    }

}
