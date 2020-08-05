package com.example.netcoders;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    Button reg, login;
    EditText name, email,password,confirm;
    TextView textView2;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        login=(Button)findViewById(R.id.login);
        reg=(Button)findViewById(R.id.reg);
        name=(EditText)findViewById(R.id.name);
        email=(EditText)findViewById(R.id.email);
        password=(EditText)findViewById(R.id.password);
        confirm=(EditText)findViewById(R.id.confirm);
        textView2=(TextView)findViewById(R.id.textView2);

        reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Name=name.getText().toString();
                String Email=email.getText().toString();
                String Password=password.getText().toString();
                String Confirm=confirm.getText().toString();
                String type="reg";
                BackgroundTask backgroundTask= new BackgroundTask(getApplicationContext());
                backgroundTask.execute(type, Name, Email, Password, Confirm);

            }
        });





    }
}
