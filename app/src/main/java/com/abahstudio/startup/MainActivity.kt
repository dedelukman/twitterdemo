package com.abahstudio.startup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_ticket.view.*
import kotlinx.android.synthetic.main.tweets_ticket.view.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    var ListTweets = ArrayList<Ticket>()
    var adapter:MyTweetsAdapter?=null
    var myemail:String?=null
    var UserUID:String?=null
    //Database instance
    private var database: FirebaseDatabase = FirebaseDatabase.getInstance();
    private var myRef=database.reference

    private var mAdView:AdView?=null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val adView = AdView(this)
        adView.adSize = AdSize.BANNER
        adView.adUnitId = "ca-app-pub-5087420313817336/4232387030"


        MobileAds.initialize(this, "ca-app-pub-5087420313817336~1442572168")

        var b:Bundle= intent.extras!!
        myemail=b.getString("email")
        UserUID=b.getString("uid")
        //Add Dummy Data
        ListTweets.add(Ticket("0","apa boa","teu nyaho","add"))



        adapter=MyTweetsAdapter(this, ListTweets)
        lvTweets.adapter=adapter

        LoadPost()
    }

    inner class MyTweetsAdapter: BaseAdapter {
        var listTweetsAdapter=ArrayList<Ticket>()
        var context: Context?=null
        constructor(context: Context, listTweetsAdapter:ArrayList<Ticket>):super(){
            this.listTweetsAdapter=listTweetsAdapter
            this.context=context
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

            var myTweet=listTweetsAdapter[position]
            val dpf= SimpleDateFormat("yyyy-MM-dd")
            val dataobj=Date()
            if (myTweet.tweetPersonUID.equals("add")){
                var myView = layoutInflater.inflate(R.layout.add_ticket,null)
                myView.iv_attach.setOnClickListener(View.OnClickListener {
                    loadImage()
                })

                myView.iv_post.setOnClickListener(View.OnClickListener {
                    myRef.child("posts").push()
                        .setValue(PostInfo(UserUID!!
                            ,myView.etPost.text.toString()
                            ,DownloadURL!!
                        ,dpf.format(dataobj)))
                    myView.etPost.setText("")

                })


                return myView

            }else if(myTweet.tweetPersonUID.equals("loading")){
                var myView = layoutInflater.inflate(R.layout.loading_ticket,null)
                return myView
            }else if(myTweet.tweetPersonUID.equals("ads")){
                var myView = layoutInflater.inflate(R.layout.ads_ticket,null)
                mAdView = myView.findViewById(R.id.adView) as AdView
                var adRequest = AdRequest.Builder().build()
                mAdView!!.loadAd(adRequest)
                return myView
            }
            else{
                var myView = layoutInflater.inflate(R.layout.tweets_ticket,null)
                myView.txt_tweet.setText(myTweet.tweetText)
                myView.txt_tweet_date.setText(myTweet.tweetDate)


                Picasso.get().load(myTweet!!.tweetImageURL).into(myView.tweet_picture);
//                Picasso.with(context).load(myTweet!!.tweetImageURL).into(myView.tweet_picture);
                myRef.child("Users").child(myTweet.tweetPersonUID!!)
                    .addValueEventListener(object : ValueEventListener{

                        override fun onDataChange(p0: DataSnapshot) {
                            try {

                                var td=p0!!.value as HashMap<String, Any>
                                for (key in td.keys){
                                    var userInfo= td[key] as String
                                    if (key.equals("ProfileImage")){
//                                        Picasso.with(context).load(userInfo).into(myView.picture_path);
                                        Picasso.get().load(userInfo).into(myView.picture_path);
                                    }else{
                                        myView.txtUserName.setText(userInfo)
                                    }
                                }

                            }catch (ex:Exception){

                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }


                    })

                return myView
            }


        }

        override fun getItem(position: Int): Any {
            return listTweetsAdapter[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return listTweetsAdapter.size
        }
    }


    //Load Image
    val  PICK_IMAGE_CODE=123
    fun loadImage(){
        var intent= Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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
            UploadImage(BitmapFactory.decodeFile(picturePath))
        }

    }

    var DownloadURL="/"
    private fun UploadImage(bitmap: Bitmap) {
        ListTweets.add(0,Ticket("0","apa boa","teu nyaho","loading"))
        adapter!!.notifyDataSetChanged()
        val storage= FirebaseStorage.getInstance()
        val storageRef=storage.getReferenceFromUrl("gs://tictactoyonline-50221.appspot.com")
        val df= SimpleDateFormat("ddMMyyHHmmss")
        val dataobj= Date()
        val imagePath=SplitString(myemail!!)+"."+df.format(dataobj)+".jpg"
        val imageRef=storageRef.child("imagePost/"+imagePath)
        val baos= ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data=baos.toByteArray()
        val uploadTask=imageRef.putBytes(data)
        uploadTask.addOnFailureListener {
            Toast.makeText(applicationContext,"fail to upload", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener {
                taskSnapshot ->
                DownloadURL = (SplitStringUrl(taskSnapshot.uploadSessionUri.toString()))
                    .replaceFirst("?name=","/")+"?alt=media"

            ListTweets.removeAt(0)
            adapter!!.notifyDataSetChanged()


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



    fun LoadPost(){
        myRef.child("posts")
            .addValueEventListener(object : ValueEventListener{

                override fun onDataChange(p0: DataSnapshot) {
                    try {
                        ListTweets.clear()
                        ListTweets.add(Ticket("0","apa boa","teu nyaho","add"))
                        ListTweets.add(Ticket("0","apa boa","teu nyaho","ads"))
                        var td=p0!!.value as HashMap<String, Any>
                        for (key in td.keys){
                            var post= td[key] as HashMap<String, Any>
                            ListTweets.add(Ticket(
                                key,
                                post["text"] as String,
                                post["postImage"] as String,
                                post["userUID"] as String,
                                        post["postDate"] as String
                            ))


                        }
                        adapter!!.notifyDataSetChanged()
                    }catch (ex:Exception){

                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }


            })
    }

}
