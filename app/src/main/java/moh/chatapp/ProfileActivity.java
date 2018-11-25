package moh.chatapp;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
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

public class ProfileActivity extends AppCompatActivity {

    private TextView mDisplayName;
    private ImageView mProfileImage;
    private TextView mProfileStatus;
    private  TextView mProfileFriendsCount;
    private Button mProfileSendRequestButton;
    private DatabaseReference mUsersDatabase;
    private ProgressDialog mProgressDialog;
    private  DatabaseReference mFriendRequestDatabase;
    private DatabaseReference mFriendDatabase;
    private FirebaseUser mUser;
    private int current_state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        final String userID = getIntent().getStringExtra("user_id");
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userID);
        mFriendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("Friends_Request");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mUser = FirebaseAuth.getInstance().getCurrentUser();

        setContentView(R.layout.activity_profile);
        mProfileImage = findViewById(R.id.profile_image);
        mProfileStatus = findViewById(R.id.profile_status);
        mProfileFriendsCount = findViewById(R.id.profile_total_friends);
        mDisplayName = findViewById(R.id.profile_display_name);
        mProfileSendRequestButton = findViewById(R.id.send_request_btn);
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

                                current_state = 3;
                                mProfileSendRequestButton.setText("Accept Friend Request");
                            } else if (request_type.equals("sent")) {
                                current_state = 2;
                                mProfileSendRequestButton.setText("Cancel Friend Request");
                            }
                            mProgressDialog.dismiss();
                        } else {
                            mFriendDatabase.child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.hasChild(userID)){
                                        current_state = 1;
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
                // NOT FRIENDS STATE
                if (current_state == 0){
                    mFriendRequestDatabase.child(mUser.getUid()).child(userID).child("request_type").setValue("sent")
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                mFriendRequestDatabase.child(userID).child(mUser.getUid()).child("request_type").setValue("received").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        mProfileSendRequestButton.setEnabled(true);
                                        current_state = 2;
                                        mProfileSendRequestButton.setText("Cancel Friend Request");
                                        Toast.makeText(ProfileActivity.this,"Request Canceled", Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                mProfileSendRequestButton.setEnabled(true);
                                Toast.makeText(ProfileActivity.this,"Failed Request", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
                // FRIEND REQUEST SENT STATE
                if (current_state == 2){
                    mFriendRequestDatabase.child(mUser.getUid()).child(userID).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendRequestDatabase.child(userID).child(mUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mProfileSendRequestButton.setEnabled(true);
                                    current_state = 0;
                                    mProfileSendRequestButton.setText("Send Friend Request");
                                    Toast.makeText(ProfileActivity.this,"Request Sent", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
                // FRIENDS STATE
                if (current_state == 3){
                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());
                    mFriendDatabase.child(mUser.getUid()).child(userID).setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendDatabase.child(userID).child(mUser.getUid()).setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mFriendRequestDatabase.child(mUser.getUid()).child(userID).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            mFriendRequestDatabase.child(userID).child(mUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    mProfileSendRequestButton.setEnabled(true);
                                                    current_state = 1;
                                                    mProfileSendRequestButton.setText("Unfriend");
                                                    Toast.makeText(ProfileActivity.this,"Request Sent", Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
                mProgressDialog.dismiss();
            }
        });
    }

}
