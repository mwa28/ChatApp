package moh.chatapp;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView mDisplayName;
    private ImageView mProfileImage;
    private TextView mProfileStatus;
    private  TextView mProfileFriendsCount;
    private Button mProfileSendRequestButton;
    private Button mProfileCancelFriendRequest;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mNotifDatabase;
    private DatabaseReference mRootRef;
    private ProgressDialog mProgressDialog;
    private  DatabaseReference mFriendRequestDatabase;
    private DatabaseReference mFriendDatabase;
    private FirebaseUser mUser;
    private int current_state;
    private int NOT_FRIENDS = 0;
    private int UNFRIEND = 1;
    private int REQUEST_SENT = 2;
    private int FRIENDS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        final String userID = getIntent().getStringExtra("user_id");
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userID);
        mFriendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("Friends_Request");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mNotifDatabase = FirebaseDatabase.getInstance().getReference().child("notification");
        setContentView(R.layout.activity_profile);
        mProfileImage = findViewById(R.id.profile_image);
        mProfileStatus = findViewById(R.id.profile_status);
        mProfileFriendsCount = findViewById(R.id.profile_total_friends);
        mDisplayName = findViewById(R.id.profile_display_name);
        mProfileSendRequestButton = findViewById(R.id.send_request_btn);
        mProfileCancelFriendRequest = findViewById(R.id.profile_decline_btn);
        mProfileCancelFriendRequest.setVisibility(View.INVISIBLE);
        mProfileCancelFriendRequest.setEnabled(false);
        current_state = 0;

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading User");
        mProgressDialog.setMessage("Please wait while we get the user");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        mUsersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String display_name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                mDisplayName.setText(display_name);
                mProfileStatus.setText(status);
                Picasso.get().load(image).placeholder(R.drawable.default_profile_picture).into(mProfileImage);

                // FRIENDS LIST / REQUEST FEATURE

                mFriendRequestDatabase.child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild(userID)){
                            String request_type = dataSnapshot.child(userID).child("request_type").getValue().toString();
                            if (request_type.equals("received")){

                                current_state = FRIENDS;
                                mProfileSendRequestButton.setText("Accept Friend Request");
                                mProfileCancelFriendRequest.setVisibility(View.INVISIBLE);
                                mProfileCancelFriendRequest.setEnabled(true);
                            } else if (request_type.equals("sent")) {
                                current_state = REQUEST_SENT;
                                mProfileSendRequestButton.setText("Cancel Friend Request");
                            }
                            mProgressDialog.dismiss();
                        } else {
                            mFriendDatabase.child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.hasChild(userID)){
                                        current_state = NOT_FRIENDS;
                                        mProfileSendRequestButton.setText("Unfriend");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    mProgressDialog.dismiss();
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        mProgressDialog.dismiss();
                    }
                });
                mProgressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mProgressDialog.dismiss();
            }
        });

        mProfileSendRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mProfileSendRequestButton.setEnabled(false);
                // NOT FRIENDS STATE --> SEND FRIEND REQUEST
                if (current_state == NOT_FRIENDS){

                    DatabaseReference newNotifRef = mRootRef.child("notification").child(userID).push();
                    String newNotifID = newNotifRef.getKey();
                    HashMap<String, String> notifData = new HashMap<>();
                    notifData.put("from", mUser.getUid());
                    notifData.put("type", "request");
                    Map requestMap = new HashMap<>();
                    requestMap.put("Friends_Request/" + mUser.getUid() + "/" + userID + "/request_type", "sent");
                    requestMap.put("Friends_Request/" + userID + "/" + mUser.getUid() + "/request_type", "received");
                    requestMap.put("notification/" + userID + "/" + newNotifID, notifData);
                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if(databaseError != null){
                                Toast.makeText(ProfileActivity.this, "Error sending a request", Toast.LENGTH_LONG).show();
                            }
                            mProfileSendRequestButton.setEnabled(true);
                            current_state = REQUEST_SENT;
                            mProfileSendRequestButton.setText("Cancel Friend Request");
                        }
                    });
                }
                // FRIEND REQUEST SENT STATE --> CANCEL FRIEND REQUEST
                if (current_state == REQUEST_SENT){
                    mFriendRequestDatabase.child(mUser.getUid()).child(userID).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendRequestDatabase.child(userID).child(mUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mProfileSendRequestButton.setEnabled(true);
                                    current_state = NOT_FRIENDS;
                                    mProfileSendRequestButton.setText("Send Friend Request");
                                    Toast.makeText(ProfileActivity.this,"Request Canceled", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
                // FRIENDS STATE --> UNFRIEND
                if (current_state == FRIENDS){
                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());
                    Map friendsMap = new HashMap();
                    friendsMap.put("Friends/" + mUser.getUid() + "/" + userID + "/date", currentDate);
                    friendsMap.put("Friends/" + userID + "/" + mUser.getUid()+ "/date", currentDate);

                    friendsMap.put("Friends_Request/" + mUser.getUid() + "/" + userID +"/date", null);
                    friendsMap.put("Friends_Request/" + userID + "/" + mUser.getUid() + "/date", null);
                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if(databaseError != null){
                                Toast.makeText(ProfileActivity.this, "Error sending a request", Toast.LENGTH_LONG).show();
                            }
                            mProfileSendRequestButton.setEnabled(true);
                            current_state = NOT_FRIENDS;
                            mProfileSendRequestButton.setText("Unfriend");
                        }
                    });

                }
                // UNFRIEND STATE --> NOT FRIENDS
                if (current_state == UNFRIEND){
                    Map unfriendMap = new HashMap();
                    unfriendMap.put("Friends/" + mUser.getUid() + "/" + userID, null);
                    unfriendMap.put("Friends/" + userID + "/" + mUser.getUid(), null);
                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if(databaseError != null){
                                Toast.makeText(ProfileActivity.this, "Error sending a request", Toast.LENGTH_LONG).show();
                            }
                            mProfileSendRequestButton.setEnabled(true);
                            current_state = NOT_FRIENDS;
                            mProfileSendRequestButton.setText("Send Friend Request");
                        }
                    });
                }
                mProgressDialog.dismiss();
            }
        });
    }

}
