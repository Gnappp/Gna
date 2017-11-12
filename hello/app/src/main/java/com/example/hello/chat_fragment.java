package com.example.hello;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.Console;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;


/**
 * Created by 진화 on 2017-07-19.
 */

public class chat_fragment extends Fragment implements View.OnClickListener{
    private static final int RC_SIGN_IN = 1001;
    private static final String FCM_MESSAGE_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = "AAAARMbdHtM:APA91bHGmVr1vNZeXifqzNybtLsyqazTIrN_htBXIMQVOB99HZk9mBFx52glvYj5w4CCHT73QOFD2CHbkVV1s1SCc-x7RLMbB3zZyp6CJ1yjCY-jYf5FVvK-n92XRFQpZhd7o8U-2xw5";

    // Firebase - Realtime Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;

    // Firebase - Authentication
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private GoogleApiClient mGoogleApiClient;

    // Views
    private ListView mListView;
    private EditText mEdtMessage;
    private ImageView mImgProfile; // 사용자 프로필 이미지 표시

    // Values
    private Chat_adapter mAdapter;
    private String userName;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        return inflater.inflate(R.layout.chat_fragment, container, false);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
        initFirebaseAuth();
        initFirebaseDatabase();
        // 뷰에 데이터를 넣는 작업 등을 할 추가할 수 있음
    }
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }
    public void initView(){
        //al = new ArrayList<String>();
        mAdapter = new Chat_adapter(getActivity(),0);
        mListView = (ListView) getView().findViewById(R.id.list_message);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final ChatData chatData = mAdapter.getItem(position);
                final FriendData friendData = new FriendData();
                FirebaseUser user = mAuth.getCurrentUser();
                if (user!=null) {
                    if (!mAuth.getCurrentUser().getEmail().equals(chatData.userEmail)) {
                        if (!TextUtils.isEmpty(chatData.userEmail)) {
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(chatData.userEmail + " 님 ")
                                    .setPositiveButton("친구추가", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            friendData.userName = chatData.userName;
                                            friendData.userPhotoUrl = chatData.userPhotoUrl;
                                            friendData.userEmail = chatData.userEmail;
                                            mFirebaseDatabase.getReference("friend")
                                                    .child(mAuth.getCurrentUser().getEmail().substring(0, mAuth.getCurrentUser().getEmail().indexOf('@')))
                                                    .child(friendData.userEmail.substring(0, friendData.userEmail.indexOf('@'))).setValue(friendData);
                                        }
                                    })
                                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // not thing..
                                        }
                                    }).show();
                        }
                    }
                }
            }
        });


        mEdtMessage = (EditText) getView().findViewById(R.id.edit_message);
        getView().findViewById(R.id.btn_send).setOnClickListener(this);
        mEdtMessage.setOnClickListener(this);
        mImgProfile = (ImageView) getView().findViewById(R.id.img_profile);

    }

    private void initFirebaseDatabase() {
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference("message");
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                ChatData chatData = dataSnapshot.getValue(ChatData.class);
                chatData.firebaseKey = dataSnapshot.getKey();

                mAdapter.add(chatData);
                mListView.smoothScrollToPosition(mAdapter.getCount());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String firebaseKey = dataSnapshot.getKey();
                int count = mAdapter.getCount();
                for (int i = 0; i < count; i++) {
                    if (mAdapter.getItem(i).firebaseKey.equals(firebaseKey)) {
                        mAdapter.remove(mAdapter.getItem(i));
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        mDatabaseReference.addChildEventListener(mChildEventListener);
    }

    private void initFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance();
        updateProfile();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                updateProfile();
            }
        };
    }

    private void updateProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            // 비 로그인 상태 (메시지를 전송할 수 없다.)
            getView().findViewById(R.id.btn_send).setVisibility(View.GONE);
            mAdapter.setEmail(null);
            mAdapter.notifyDataSetChanged();
        } else {
            // 로그인 상태


            getView().findViewById(R.id.btn_send).setVisibility(View.VISIBLE);

            userName = user.getDisplayName(); // 채팅에 사용 될 닉네임 설정
            String email = user.getEmail();
            StringBuilder profile = new StringBuilder();
            profile.append(userName).append("\n").append(user.getEmail());
            mAdapter.setEmail(email);
            mAdapter.notifyDataSetChanged();

//            Picasso.with(getActivity()).load(user.getPhotoUrl()).into(mImgProfile);

            /*UserData userData = new UserData();
            userData.userEmailID = email.substring(0, email.indexOf('@'));
            userData.fcmToken = FirebaseInstanceId.getInstance().getToken();

            //mFirebaseDatabase.getReference("users").setValue(userData);*/
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthListener);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                mAuth=FirebaseAuth.getInstance();
                updateProfile();
                if(mAuth.getCurrentUser()==null)
                    break;
                String message = mEdtMessage.getText().toString();
                if (!TextUtils.isEmpty(message)) {
                    mEdtMessage.setText("");
                    ChatData chatData = new ChatData();
                    chatData.userName = userName;
                    chatData.message = message;
                    chatData.time = System.currentTimeMillis();
                    chatData.userEmail = mAuth.getCurrentUser().getEmail(); // 사용자 이메일 주소
                    chatData.userPhotoUrl = mAuth.getCurrentUser().getPhotoUrl().toString(); // 사용자 프로필 이미지 주소
                    mDatabaseReference.push().setValue(chatData);
                }
                break;
            case R.id.edit_message:
                mAuth=FirebaseAuth.getInstance();
                updateProfile();
                break;
        }

    }


}
