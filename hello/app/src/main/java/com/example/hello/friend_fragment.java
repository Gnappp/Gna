package com.example.hello;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by 진화 on 2017-07-19.
 */

public class friend_fragment extends Fragment {

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

    // Values
    private Friend_adapter mAdapter;
    private String userName;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.friend_fragment, container, false);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
        initFirebaseAuth();
        //initFirebaseDatabase();
        // 뷰에 데이터를 넣는 작업 등을 할 추가할 수 있음
    }

    public void initView(){
        //al = new ArrayList<String>();
        mAdapter = new Friend_adapter(getActivity(),0);
        mListView = (ListView) getView().findViewById(R.id.list_friend);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final FriendData friendData = mAdapter.getItem(position);
                if (!TextUtils.isEmpty(friendData.userEmail)) {
                    final EditText editText = new EditText(getActivity());
                    new AlertDialog.Builder(getActivity())
                            .setMessage(friendData.userEmail + " 님 에게 메시지 보내기")
                            .setView(editText)
                            .setPositiveButton("보내기", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sendPostToFCM(friendData, editText.getText().toString());
                                }
                            })
                            .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // not thing..
                                }
                            })
                            .setNeutralButton("삭제", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mFirebaseDatabase.getReference("friend").child(mAuth.getCurrentUser().getEmail().substring(0, mAuth.getCurrentUser().getEmail().indexOf('@')))
                                            .child(friendData.userEmail.substring(0, friendData.userEmail.indexOf('@'))).removeValue();
                                    // not thing..
                                }
                            }).show();

                }
            }
        });
    }

    private void initFirebaseDatabase() {
        if(mAuth.getCurrentUser() !=null ) {
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            mDatabaseReference = mFirebaseDatabase.getReference("friend").child(mAuth.getCurrentUser().getEmail().substring(0, mAuth.getCurrentUser().getEmail().indexOf('@')));
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    FriendData friendData = dataSnapshot.getValue(FriendData.class);
                    friendData.firebaseKey = dataSnapshot.getKey();
                    Toast.makeText(getActivity(), dataSnapshot.getValue().toString().substring(1, dataSnapshot.getValue().toString().indexOf('=')), Toast.LENGTH_SHORT).show();
                    mAdapter.add(friendData);
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
    }

    private void initFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance();
        //updateProfile();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                updateProfile();
            }
        };
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


    private void updateProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            // 비 로그인 상태 (메시지를 전송할 수 없다.)
            mAdapter.clear();
        }
        else {
            // 로그인 상태
            mAdapter.clear();
            initFirebaseDatabase();
        }
    }

    private void sendPostToFCM(final FriendData friendData, final String message) {
        mFirebaseDatabase.getReference("users")
                .child(friendData.userEmail.substring(0, friendData.userEmail.indexOf('@')))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final UserData userData = dataSnapshot.getValue(UserData.class);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // FMC 메시지 생성 start
                                    JSONObject root = new JSONObject();
                                    JSONObject notification = new JSONObject();
                                    notification.put("body", message);
                                    notification.put("title", getString(R.string.app_name));
                                    root.put("notification", notification);
                                    root.put("to", userData.fcmToken);
                                    // FMC 메시지 생성 end

                                    URL Url = new URL(FCM_MESSAGE_URL);
                                    HttpURLConnection conn = (HttpURLConnection) Url.openConnection();
                                    conn.setRequestMethod("POST");
                                    conn.setDoOutput(true);
                                    conn.setDoInput(true);
                                    conn.addRequestProperty("Authorization", "key=" + SERVER_KEY);
                                    conn.setRequestProperty("Accept", "application/json");
                                    conn.setRequestProperty("Content-type", "application/json");
                                    OutputStream os = conn.getOutputStream();
                                    os.write(root.toString().getBytes("utf-8"));
                                    os.flush();
                                    conn.getResponseCode();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }


}
