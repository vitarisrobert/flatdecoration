package flatdecoration.shop;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegistrationActivity extends AppCompatActivity {

    private static final String LOG_TAG = RegistrationActivity.class.getName();
    private static final int SECRET_KEY = 97;


    EditText editText_Username;
    EditText editText_Email;
    EditText editText_Password;
    EditText editText_PasswordAgain;
    EditText editText_Phone;
    RadioGroup customerTypeGroup;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        int secret_key = getIntent().getIntExtra("SECRET_KEY", 0);

        if(secret_key != 97){
            finish();
        }

        editText_Username = findViewById(R.id.editText_Username);
        editText_Email = findViewById(R.id.editText_Email);
        editText_Password = findViewById(R.id.editText_Password);
        editText_PasswordAgain = findViewById(R.id.editText_PasswordAgain);
        editText_Phone = findViewById(R.id.editText_Phone);
        customerTypeGroup = findViewById(R.id.customerTypeGroup);
        customerTypeGroup.check(R.id.radioButton_PivatePerson);

        mAuth = FirebaseAuth.getInstance();

    }

    public void registration(View view) {
        String username = editText_Username.getText().toString();
        String email = editText_Email.getText().toString();
        String password = editText_Password.getText().toString();
        String passwordAgain = editText_PasswordAgain.getText().toString();
        String phone = editText_Phone.getText().toString();

        if(!password.equals(passwordAgain)){
            Log.e(LOG_TAG, "Nem egyforma a két jelszó!");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Log.d(LOG_TAG, "Sikeres felhasználó létrehozás!");
                    login();
                }else{
                    Log.d(LOG_TAG, "Sikertelen felhasználó létrehozás!");
                    Toast.makeText(RegistrationActivity.this, "Sikertelen felhasználó létrehozás: " + task.getException().getMessage(),Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void login() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("SECRET_KEY", SECRET_KEY);
        startActivity(intent);
    }

    public void cancel(View view) {
        finish();
    }
}