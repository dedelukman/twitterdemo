package com.abahstudio.startup

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class Login : AppCompatActivity() {


    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var mAuth: FirebaseAuth? = null
    //Database instance
    private var database: FirebaseDatabase = FirebaseDatabase.getInstance();
    private var myRef=database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        ivPerson.setOnClickListener (View.OnClickListener {
            checkPermission()
        })
    }

    val READIMAGE:Int=253
    fun checkPermission(){
        if (Build.VERSION.SDK_INT>=23){
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)!=
                    PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),READIMAGE)
            return
        }
        loadImage()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READIMAGE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadImage()
                } else {
                    Toast.makeText(this, "Cannot acces your images", Toast.LENGTH_LONG).show()
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    val  PICK_IMAGE_CODE=123
    fun loadImage(){
        var intent=Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(intent,PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==PICK_IMAGE_CODE && data!=null && resultCode== Activity.RESULT_OK){
            val selectedImage=data.data
            val filePathColum= arrayOf(MediaStore.Images.Media.DATA)
            val cursor=contentResolver.query(selectedImage!!,filePathColum,null,null,null)
            cursor!!.moveToFirst()
            val columIndex=cursor.getColumnIndex(filePathColum[0])
            val picturePath=cursor.getString(columIndex)
            cursor.close()
            ivPerson.setImageBitmap(BitmapFactory.decodeFile(picturePath))
        }

    }

    fun buLogin(view: View) {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()
        signUpFirebase(email,password)
    }

    fun signUpFirebase(email:String, password:String){
        mAuth!!.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(
                this
            ) { task ->
                if (task.isSuccessful) { // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(applicationContext,"Successful Sign Up", Toast.LENGTH_LONG).show()
//                    val currentUser =mAuth!!.currentUser
//                    Log.d("Login:",currentUser!!.uid)
                    SaveImageInFirebase()
                } else { // If sign in fails, display a message to the user.
                    Toast.makeText(applicationContext,"Fail Sign Up",Toast.LENGTH_LONG).show()
                }
                // ...
            }
    }

    fun signInFirebase(email: String, password: String){
        mAuth!!.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(
                this
            ) { task ->
                if (task.isSuccessful) { // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(applicationContext,"Successful login", Toast.LENGTH_LONG).show()
//                    val currentUser =mAuth!!.currentUser
//                    Log.d("Login:",currentUser!!.uid)
                    SaveImageInFirebase()
                } else { // If sign in fails, display a message to the user.
                    Toast.makeText(applicationContext,"Fail login",Toast.LENGTH_LONG).show()
                }
                // ...
            }
    }

    fun SaveImageInFirebase(){
        var currentUser= mAuth!!.currentUser
        val email:String=currentUser!!.email.toString()
        val storage=FirebaseStorage.getInstance()
        val storageRef=storage.getReferenceFromUrl("gs://tictactoyonline-50221.appspot.com")
        val df= SimpleDateFormat("ddMMyyHHmmss")
        val dpf= SimpleDateFormat("yyyy-MM-dd")
        val dataobj=Date()
        val imagePath=SplitString(email)+"."+df.format(dataobj)+".jpg"
        val imageRef=storageRef.child("images/"+imagePath)
        @Suppress("DEPRECATION")
        ivPerson.isDrawingCacheEnabled=true
        @Suppress("DEPRECATION")
        ivPerson.buildDrawingCache()

        val drawble=ivPerson.drawable as BitmapDrawable
        val bitmap=drawble.bitmap
        val baos=ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data=baos.toByteArray()
        val uploadTask=imageRef.putBytes(data)
        uploadTask.addOnFailureListener {
            Toast.makeText(applicationContext,"fail to upload", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener {
           taskSnapshot ->
//            var DownloadURL = taskSnapshot.storage.downloadUrl.toString()!!
//            var DownloadURL=imageRef.toString()
            var DownloadURL = (SplitStringUrl(taskSnapshot.uploadSessionUri.toString())).replaceFirst("?name=","/")+"?alt=media"
            if(DownloadURL==null){
                DownloadURL="/"
            }
            myRef.child("Users").child(currentUser.uid).child("email").setValue(currentUser.email)
            myRef.child("Users").child(currentUser.uid).child("date").setValue(dpf.format(dataobj))
            myRef.child("Users").child(currentUser.uid).child("ProfileImage").setValue(DownloadURL)
            LoadTweets()
        }

    }

    fun SplitString(email: String):String{
        val split=email.split("@")
        return split[0]
    }
    fun SplitStringUrl(email: String):String{
        val split=email.split("&")
        return split[0]
    }

    fun LoadTweets(){
        var currentUser =mAuth!!.currentUser

        if(currentUser!=null) {


            var intent = Intent(this, MainActivity::class.java)
            intent.putExtra("email", currentUser.email)
            intent.putExtra("uid", currentUser.uid)

            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        LoadTweets()
    }
}
