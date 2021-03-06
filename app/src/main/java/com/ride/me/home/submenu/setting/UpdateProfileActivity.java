package com.ride.me.home.submenu.setting;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.ride.me.GoridemeAplication;
import com.ride.me.api.ServiceGenerator;
import com.ride.me.api.service.UserService;
import com.ride.me.model.FirebaseToken;
import com.ride.me.model.User;
import com.ride.me.model.json.user.LoginRequestJson;
import com.ride.me.model.json.user.LoginResponseJson;
import com.ride.me.model.json.user.UpdateProfileRequestJson;
import com.ride.me.model.json.user.UpdateProfileResponseJson;
import com.ride.me.utils.DialogActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateProfileActivity extends DialogActivity {
    @BindView(com.ride.me.R.id.first_name)
    EditText firstName;
    @BindView(com.ride.me.R.id.btn_home)
    ImageView backButton;
    @BindView(com.ride.me.R.id.last_name)
    EditText lastName;
    @BindView(com.ride.me.R.id.email)
    EditText email;
    @BindView(com.ride.me.R.id.phone)
    EditText phone;
    @BindView(com.ride.me.R.id.save_profile)
    Button saveProfile;

    String username = "";
    String password = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.ride.me.R.layout.activity_update_profile);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(com.ride.me.R.id.toolbar);
        setSupportActionBar(toolbar);
        final User user = GoridemeAplication.getInstance(this).getLoginUser();
        firstName.setText(user.getNamaDepan());
        lastName.setText(user.getNamaBelakang());
        email.setText(user.getEmail());
        phone.setText(user.getNoTelepon());

        username = user.getEmail();
        password = user.getPassword();

        saveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressDialog("Updating...");
                UpdateProfileRequestJson request = new UpdateProfileRequestJson();
                request.id = user.getId();
                request.email = email.getText().toString();
                request.nama_depan = firstName.getText().toString();
                request.nama_belakang = lastName.getText().toString();
                request.no_telepon = phone.getText().toString();
                request.tgl_lahir = user.getTglLahir();
                request.tempat_lahir = user.getTempatLahir();
                request.alamat = user.getAlamat();


                UserService service = ServiceGenerator.createService(UserService.class, user.getEmail(), user.getPassword());
                service.updateProfile(request).enqueue(new Callback<UpdateProfileResponseJson>() {
                    @Override
                    public void onResponse(Call<UpdateProfileResponseJson> call, Response<UpdateProfileResponseJson> response) {
                        hideProgressDialog();
                        if (response.isSuccessful()) {
                            if (response.body().message.equals("success")) {
                                //update_data();
                                Toast.makeText(UpdateProfileActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                                Realm realm = GoridemeAplication.getInstance(UpdateProfileActivity.this).getRealmInstance();
                                realm.beginTransaction();
                                GoridemeAplication.getInstance(UpdateProfileActivity.this).getLoginUser().setNamaDepan(firstName.getText().toString());
                                GoridemeAplication.getInstance(UpdateProfileActivity.this).getLoginUser().setNamaBelakang(lastName.getText().toString());
                                realm.commitTransaction();
                            } else {
                                Toast.makeText(UpdateProfileActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<UpdateProfileResponseJson> call, Throwable t) {
                        hideProgressDialog();
                        t.printStackTrace();
                        Toast.makeText(UpdateProfileActivity.this, "System error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    private void update_data() {
        showProgressDialog(com.ride.me.R.string.dialog_loading);
        LoginRequestJson request = new LoginRequestJson();
        request.setEmail(username);
        request.setPassword(password);

        Realm realm = Realm.getDefaultInstance();
        FirebaseToken token = realm.where(FirebaseToken.class).findFirst();
        if (token.getTokenId() != null) {
            request.setRegId(token.getTokenId());
        } else {
            Toast.makeText(this, com.ride.me.R.string.waiting_pleaseWait, Toast.LENGTH_SHORT).show();
            hideProgressDialog();
            return;
        }

        UserService service = ServiceGenerator.createService(UserService.class, request.getEmail(), request.getPassword());
        service.login(request).enqueue(new Callback<LoginResponseJson>() {
            @Override
            public void onResponse(Call<LoginResponseJson> call, Response<LoginResponseJson> response) {
                hideProgressDialog();
                if (response.isSuccessful()) {
                    if (response.body().getMessage().equalsIgnoreCase("found")) {
                        User user = response.body().getData().get(0);

                        saveUser(user);

                        Intent intent = new Intent(UpdateProfileActivity.this, UpdateProfileActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(UpdateProfileActivity.this, "Username atau Password salah", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponseJson> call, Throwable t) {
                hideProgressDialog();
                t.printStackTrace();
                Toast.makeText(UpdateProfileActivity.this, "System error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUser(User user) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.delete(User.class);
        realm.copyToRealm(user);
        realm.commitTransaction();

        GoridemeAplication.getInstance(UpdateProfileActivity.this).setLoginUser(user);
    }

}
