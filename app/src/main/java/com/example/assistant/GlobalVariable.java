package com.example.assistant;

import android.app.Application;
import com.example.assistant.data.model.User;

public class GlobalVariable extends Application {
    private String ast_num;
    private String topic_nam;
    private String user_name;

    public static final String DB_URL = BuildConfig.KEBBI_API_BASE_URL;

    private User currentUser;
    public void setAst_num (String ast_num) {
        this.ast_num = ast_num;
    }

    public String getAst_num () {
        return ast_num;
    }

    public void setTopic_nam (String topic_nam)
    {
        this.topic_nam = topic_nam;
    }

    public String getTopic_nam ()
    {
        return topic_nam;
    }

    public void setUser_name (String user_name) {
        this.user_name = user_name;
    }

    // --- 新增的方法 ---
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        this.currentUser = null;
    }

    public String getUser_name () {
        return user_name;
    }

    public int RanNum () {
        int i;
        i = (int)(Math.random()*10)+1;
        return i;
    }

    public String SelectMotion (int i) {
        switch (i) {
            case 1 :
                return "666_BA_RHandS03";
            case 2 :
                return "666_BA_RzArmS45";
            case 3 :
                return "666_BA_RzArmS90";
            case 4 :
                return "666_DA_Bathe";
            case 5 :
                return "666_DA_Eat";
            case 6 :
                return "666_DA_Excrement";
            case 7 :
                return "666_DA_Hit";
            case 8 :
                return "666_DA_PickUp";
            case 9 :
                return "666_DA_Press";
            case 10 :
                return "666_DA_Splash";
        }
        return "";
    }
    public int MouthRanNum() {
        int i;
        i = (int)(Math.random()*4)+1;
        return i;
    }

    public int FaceRanNum() {
        int i;
        i = (int)(Math.random()*7)+1;
        return i;
    }

    public int SelectMouth (int i) {
        int type = 8;
        switch (i) {
            case 1 :
                type = 1;
                break;
            case 2 :
                type = 2;
                break;
            case 3 :
                type = 3;
                break;
        }
        return type;
    }

    public String SelectEmoji (int i) {
        switch (i) {
            case 1 :
                return "TTS_JoyA";
            case 2 :
                return "TTS_JoyB";
            case 3 :
                return "TTS_JoyC";
            case 4 :
                return "TTS_PeaceA";
            case 5 :
                return "TTS_PeaceB";
            case 6 :
                return "TTS_PeaceC";
        }
        return "TTS_Surprise";
    }

}
