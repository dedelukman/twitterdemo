package com.abahstudio.startup

class PostInfo {
    var UserUID:String?=null
    var Text:String?=null
    var PostImage:String?=null
    var PostDate:String?=null

    constructor(UserUID:String,Text:String,PostImage:String){
        this.UserUID=UserUID
        this.Text=Text
        this.PostImage=PostImage
    }
    constructor(UserUID:String,Text:String,PostImage:String,PostDate:String){
        this.UserUID=UserUID
        this.Text=Text
        this.PostImage=PostImage
        this.PostDate=PostDate
    }

}