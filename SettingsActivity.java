package com.example.encryptedchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {

    private DatabaseReference mUserDatabase;
    private FirebaseUser mcurrentUser;

    //layout
    private CircleImageView mImage;
    private TextView mName;
    private TextView mStatus;
    private Button mImageBtn;
    private Button mStatusBtn;
//    private static  final int GALLERY_PICK = 1;

    //storage firebase
private StorageReference mImageStorage;
private ProgressDialog mProgressDialog;
private UploadTask uploadTask;
private  String displayimageUrl;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mImage = findViewById(R.id.circleImageView);
        mName = findViewById(R.id.account_display_name);
        mStatus = findViewById(R.id.account_settins_status);
        mImageBtn = findViewById(R.id.btn_changeimg);
        mStatusBtn = findViewById(R.id.btn_changestatus);
        mImageStorage = FirebaseStorage.getInstance().getReference();



        //retrieving data from firebase


        mcurrentUser= FirebaseAuth.getInstance().getCurrentUser();
        String currentUID = mcurrentUser.getUid();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUID);
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue().toString();
                String image = snapshot.child("image").getValue().toString();
                String status = snapshot.child("status").getValue().toString();
                String thumb_image = snapshot.child("thumb_image").getValue().toString();
                mName.setText(name);
                mStatus.setText(status);
                if (!image.equals("default")) {
                    Picasso.get().load(image).placeholder(R.drawable.default_imgh).into(mImage);


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        mStatusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String status_value = mStatus.getText().toString();

                Intent status_Intent = new Intent(SettingsActivity.this,StatusActivity.class);
                status_Intent.putExtra("status_value",status_value);

                startActivity(status_Intent);

            }
        });

        mImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // start picker to get image for cropping and then use the image in cropping activity
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1,1)
                        .start(SettingsActivity.this);
//                Intent galleryIntent = new Intent();
//                galleryIntent.setType("image/*");
//                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
//                startActivityForResult(Intent.createChooser(galleryIntent,"SELECT IMAGE"),GALLERY_PICK);

            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mProgressDialog = new ProgressDialog(SettingsActivity.this);
                mProgressDialog.setTitle("Uploading Image...");
                mProgressDialog.setMessage("Please wait while we uplaod and process the image.");
                mProgressDialog.show();
                Uri resultUri = result.getUri();
                File thumb_filepath = new File(resultUri.getPath());
                Bitmap thumb_bitmap = new Compressor(this)
                        .setMaxWidth(200)
                        .setMaxHeight(200)
                        .setQuality(75)
                        .compressToBitmap(thumb_filepath);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumb_bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos);
                final byte[] thumb_byte = baos.toByteArray();



                String currentUID = mcurrentUser.getUid();
                final StorageReference thumb_pathInFirebae =mImageStorage.child("profile_images").child("thumbs").child(currentUID + ".jpg");
                final StorageReference filepath = mImageStorage.child("profile_images").child(currentUID+ ".jpg");
                filepath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful())
                        {
                           mProgressDialog.dismiss();
                            Toast.makeText(SettingsActivity.this, "Uploaded Successfully", Toast.LENGTH_LONG).show();
                            //https://stackoverflow.com/questions/50554548/error-cannot-find-symbol-method-getdownloadurl-of-type-com-google-firebase-st
                            filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    final String download_url = uri.toString();
                                    UploadTask uploadTask = thumb_pathInFirebae.putBytes(thumb_byte);
                                    uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                            if(task.isSuccessful())
                                            {
                                                thumb_pathInFirebae.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                    @Override
                                                    public void onSuccess(Uri uri) {
                                                        String thumb_downloadUrl = uri.toString();
                                                        Map update_hashMap = new HashMap<>();
                                                        update_hashMap.put("image",download_url);
                                                        update_hashMap.put("thumb_image",thumb_downloadUrl);
                                                        mUserDatabase.updateChildren(update_hashMap);


                                                    }
                                                });




                                            }

                                        }
                                    });



                                }
                            });

                        }
                        else {

                            Toast.makeText(SettingsActivity.this, "Error", Toast.LENGTH_LONG).show();
                            mProgressDialog.dismiss();

                        }

                    }
                });




            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }


}