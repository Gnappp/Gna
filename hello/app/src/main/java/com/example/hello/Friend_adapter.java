package com.example.hello;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by 진화 on 2017-07-25.
 */

public class Friend_adapter extends ArrayAdapter<FriendData> {
    private final static int TYPE_MY_SELF = 0;
    private final static int TYPE_ANOTHER = 1;


    public Friend_adapter(Context context, int resource) {
        super(context, resource);
    }


    private View setAnotherView(LayoutInflater inflater) {
        View convertView = inflater.inflate(R.layout.friend_item, null);
        Friend_adapter.ViewHolderAnother holder = new Friend_adapter.ViewHolderAnother();
        holder.bindView(convertView);
        convertView.setTag(holder);
        return convertView;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        if (convertView == null) {
            if (viewType == TYPE_ANOTHER) {
                convertView = setAnotherView(inflater);
            }
        }

        if (convertView.getTag() instanceof Friend_adapter.ViewHolderAnother) {
            if (viewType != TYPE_ANOTHER) {
                convertView = setAnotherView(inflater);
            }
            ((Friend_adapter.ViewHolderAnother) convertView.getTag()).setData(position);
        }

        return convertView;
    }


    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {

            return TYPE_ANOTHER; // 상대방의 채팅내용
    }

    /*public boolean checkOverlap(String email){
        for(int pos=0;pos<this.getCount();pos++){
            String useremail=this.getItem(pos).userEmail;
            if(this.getItem(pos).userEmail.equals(email)){
                return true;
            }
        }
        return false;
    }*/

    private class ViewHolderAnother {
        private ImageView mImgProfile;
        private TextView mTxtUserName;
        private TextView mTxtMessage;
        private TextView mTxtTime;

        private void bindView(View convertView) {
            mImgProfile = (ImageView) convertView.findViewById(R.id.img_profile);
            mTxtUserName = (TextView) convertView.findViewById(R.id.txt_userName);
            mTxtMessage = (TextView) convertView.findViewById(R.id.txt_email);

        }

        private void setData(int position) {
            FriendData chatData = getItem(position);
            Picasso.with(getContext()).load(chatData.userPhotoUrl).into(mImgProfile);
            mTxtUserName.setText(chatData.userName);
            mTxtMessage.setText(chatData.userEmail);
        }
    }



}