package moh.chatapp;

import android.content.Intent;
import android.os.TestLooperManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.w3c.dom.Text;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout mDisplayName;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private Button mCreateBtn;

    private FirebaseAuth mAuth;
    private Toolbar mToolarBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mToolarBar = (Toolbar) findViewById(R.id.register_toolbar);
        setSupportActionBar(mToolarBar);
        getSupportActionBar().setTitle("Create an Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();

        mDisplayName = (TextInputLayout) findViewById(R.id.reg_display_name);
        mEmail = (TextInputLayout) findViewById(R.id.reg_email);
        mPassword = (TextInputLayout) findViewById(R.id.reg_password);
        mCreateBtn = (Button) findViewById(R.id.reg_create_btn);

        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String display_name = mDisplayName.getEditText().getText().toString();
                String email = mEmail.getEditText().getText().toString();
                String password = mPassword.getEditText().getText().toString();

                register_user(display_name,email,password);
            }
        });
    }

    private void register_user(String display_name, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Error", Toast.LENGTH_LONG).show();

                }
            }
        });
    }
}
